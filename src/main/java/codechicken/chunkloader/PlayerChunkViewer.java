package codechicken.chunkloader;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.WorldClient;
import codechicken.lib.packet.PacketCustom;
import codechicken.lib.vec.Vector3;

@SuppressWarnings("serial")
public class PlayerChunkViewer extends JFrame
{
    public static class TicketInfo
    {
        int ID;
        String modId;
        String player;
        net.minecraftforge.common.ForgeChunkManager.Type type;
        Entity entity;
        Set<ChunkCoordIntPair> chunkSet;

        public TicketInfo(PacketCustom packet, WorldClient world)
        {
            ID = packet.readInt();
            modId = packet.readString();
            if(packet.readBoolean())
                player = packet.readString();
            type = net.minecraftforge.common.ForgeChunkManager.Type.values()[packet.readUByte()];
            if(type == net.minecraftforge.common.ForgeChunkManager.Type.ENTITY)
                entity = world.getEntityByID(packet.readInt());
            int chunks = packet.readUShort();
            chunkSet = new HashSet<ChunkCoordIntPair>(chunks);
            for(int i = 0; i < chunks; i++)
            {
                chunkSet.add(new ChunkCoordIntPair(packet.readInt(), packet.readInt()));
            }
        }
    }

    public static class PlayerInfo
    {
        public PlayerInfo(String username2)
        {
            this.username = username2;
        }

        final String username;
        Vector3 position;
        int dimension;
    }

    public static class DimensionChunkInfo
    {
        public final int dimension;
        public HashSet<ChunkCoordIntPair> allchunks = new HashSet<ChunkCoordIntPair>();
        public HashMap<Integer, TicketInfo> tickets = new HashMap<Integer, TicketInfo>();

        public DimensionChunkInfo(int dim)
        {
            dimension = dim;
        }
    }

    public class TicketInfoDialog extends JDialog implements LayoutManager
    {
        private LinkedList<TicketInfo> tickets;

        private JTextPane infoPane;
        private JScrollPane infoScrollPane;
        private JTextPane chunkPane;
        private JScrollPane chunkScrollPane;
        private JComboBox<String> ticketComboBox;

        public TicketInfoDialog(LinkedList<TicketInfo> tickets)
        {
            super(PlayerChunkViewer.this);
            setModalityType(ModalityType.DOCUMENT_MODAL);
            this.tickets = tickets;

            infoPane = new JTextPane();
            infoPane.setEditable(false);
            infoPane.setOpaque(false);
            infoPane.setContentType("text/html");
            
            infoScrollPane = new JScrollPane(infoPane);
            infoScrollPane.setOpaque(false);
            add(infoScrollPane);
            
            chunkPane = new JTextPane();
            chunkPane.setEditable(false);
            chunkPane.setOpaque(false);
            chunkPane.setContentType("text/html");
            
            chunkScrollPane = new JScrollPane(chunkPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            add(chunkScrollPane);

            ticketComboBox = new JComboBox<String>();
            for(TicketInfo ticket : tickets)
            {
                String ident = ticket.modId;
                if(ticket.player != null)
                    ident += ", " + ticket.player;
                ident += " #" + ticket.ID;
                ticketComboBox.addItem(ident);
            }
            add(ticketComboBox);

            addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                    dialog = null;
                }
            });

            setLayout(this);
            setSize(getPreferredSize());
            setLocationRelativeTo(null);
            pack();

            dialog = this;

            setVisible(true);
        }

        public void update()
        {
            TicketInfo ticket = tickets.get(ticketComboBox.getSelectedIndex());
            String info = "<span style=\"font-family:Tahoma; font-size:10px\">";
            info += "Mod: " + ticket.modId;
            if(ticket.player != null)
                info += "<br>Player: " + ticket.player;
            info += "<br>Type: " + ticket.type.name();
            if(ticket.entity != null)
                info += "<br>Entity: " + EntityList.classToStringMapping.get(ticket.entity) + "#" + ticket.entity.getEntityId() + " (" + String.format("%.2f", ticket.entity.posX) + ", " + String.format("%.2f", ticket.entity.posY) + ", " + String.format("%.2f", ticket.entity.posZ) + ")";
            info+="</span><p style=\"text-align:center; font-family:Tahoma; font-size:10px\">ForcedChunks</p>";
            String chunks = "<span style=\"font-family:Tahoma; font-size:10px\">";
            for(ChunkCoordIntPair coord : ticket.chunkSet)
                chunks += coord.chunkXPos + ", " + coord.chunkZPos + "<br>";
            chunks += "</span>";
            infoPane.setText(info);
            chunkPane.setText(chunks);
            repaint();
        }

        @Override
        public void layoutContainer(Container parent)
        {
            Dimension size = parent.getSize();
            ticketComboBox.setBounds(40, 20, size.width - 80, 20);
            int w = size.width - 40;
            int y = 60;
            infoPane.setBounds(5, 5, w-10, 5);
            chunkPane.setBounds(5, 5, w-10, 5);
            infoScrollPane.setBounds(20, y, w, Math.min(size.height - 80, infoPane.getPreferredSize().height+10));
            y += 10+infoScrollPane.getHeight();
            chunkScrollPane.setBounds(20, y, w, Math.max(0, size.height-40-y));
        }

        @Override
        public void addLayoutComponent(String name, Component comp)
        {
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return new Dimension(250, 300);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            return new Dimension(250, 300);
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
        }
    }

    private HashMap<String, PlayerInfo> players = new HashMap<String, PlayerInfo>();
    private final HashMap<Integer, DimensionChunkInfo> dimensionChunks = new HashMap<Integer, DimensionChunkInfo>();

    private int dimension = 0;
    private int xCenter = 0;
    private int zCenter = 0;
    private DisplayArea displayArea;
    private JComboBox<Integer> dimComboBox;
    private JLabel dimLabel;
    private JTextField xArea;
    private JLabel xLabel;
    private JTextField zArea;
    private JLabel zLabel;
    public TicketInfoDialog dialog;

    public static void openViewer(int x, int z, int dim)
    {
        instance = new PlayerChunkViewer(x, z, dim);
    }

    private PlayerChunkViewer(int x, int z, int dim)
    {
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if(Minecraft.getMinecraft().getNetHandler() != null)
                    ChunkLoaderCPH.sendGuiClosing();
            }
        });

        setLayout(new LayoutManager()
        {
            @Override
            public void removeLayoutComponent(Component paramComponent)
            {
            }

            @Override
            public Dimension preferredLayoutSize(Container paramContainer)
            {
                return new Dimension(500, 500);
            }

            @Override
            public Dimension minimumLayoutSize(Container paramContainer)
            {
                return null;
            }

            @Override
            public void layoutContainer(Container paramContainer)
            {
                int width = getRootPane().getWidth();
                int height = getRootPane().getHeight();

                xLabel.setBounds(20, 10, 50, 20);
                xArea.setBounds(70, 10, 60, 20);
                zLabel.setBounds(140, 10, 50, 20);
                zArea.setBounds(190, 10, 60, 20);
                dimLabel.setBounds(260, 10, 60, 20);
                dimComboBox.setBounds(330, 10, 70, 20);
                displayArea.setBounds(0, 40, width, height - 40);
            }

            @Override
            public void addLayoutComponent(String paramString, Component paramComponent)
            {
            }
        });

        ToolTipManager.sharedInstance().setEnabled(true);
        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setReshowDelay(100);
        addComponents();
        pack();
        setTitle("Chunk Viewer");

        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        int width = 500;
        int height = 500;
        setCenter(x, z);
        dimension = dim;
        setBounds(p.x - width / 2, p.y - height / 2, width, height);

        setBackground(new Color(1F, 1, 1));

        setVisible(true);

        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                startUpdateThread();
            }
        });
    }

    protected void startUpdateThread()
    {
        new Thread("Info Frame Update Thread")
        {
            public void run()
            {
                while(true)
                {
                    if(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu)
                        dispose();

                    if(instance == null || !isVisible())
                        return;

                    update();
                    if(dialog != null)
                        dialog.update();

                    try
                    {
                        Thread.sleep(50);
                    }
                    catch(InterruptedException e)
                    {
                    }
                }
            }
        }.start();
    }

    protected void update()
    {
        int selectedDim = dimension;
        boolean needsReset = false;
        LinkedList<Integer> dims = new LinkedList<Integer>(dimensionChunks.keySet());
        Collections.sort(dims);
        if(dims.size() != dimComboBox.getItemCount())
            needsReset = true;
        else
        {
            for(int index = 0; index < dimComboBox.getItemCount();)
            {
                if(!dims.get(index).equals(dimComboBox.getItemAt(index)))
                {
                    needsReset = true;
                    break;
                }
                index++;
            }
        }

        if(needsReset)
        {
            dimComboBox.removeAllItems();
            dims = new LinkedList<Integer>(dimensionChunks.keySet());
            Collections.sort(dims);
            for(int dim : dims)
            {
                dimComboBox.addItem(dim);
            }

            if(dims.contains(selectedDim))
                dimComboBox.setSelectedItem(selectedDim);
        }
        repaint();
    }

    private void addComponents()
    {
        add(getDisplayArea());
        add(getXLabel());
        add(getXArea());
        add(getZLabel());
        add(getZArea());
        add(getDimLabel());
        add(getDimComboBox());
    }

    public DisplayArea getDisplayArea()
    {
        if(displayArea == null)
        {
            displayArea = new DisplayArea();
            displayArea.addMouseListener(displayArea);
            displayArea.addMouseMotionListener(displayArea);
        }
        return displayArea;
    }

    public JLabel getXLabel()
    {
        if(xLabel == null)
        {
            xLabel = new JLabel("xCenter");
        }
        return xLabel;
    }

    public JTextField getXArea()
    {
        if(xArea == null)
        {
            xArea = new JTextField();
            xArea.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    try {
                        setCenter(Integer.parseInt(xArea.getText()), zCenter);
                    } catch (NumberFormatException ignored) {
                    }
                }
            });
        }
        return xArea;
    }

    public JLabel getZLabel()
    {
        if(zLabel == null)
        {
            zLabel = new JLabel("zCenter");
        }
        return zLabel;
    }

    public JTextField getZArea()
    {
        if(zArea == null)
        {
            zArea = new JTextField();
            zArea.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    try {
                        setCenter(xCenter, Integer.parseInt(zArea.getText()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            });
        }
        return zArea;
    }

    public JLabel getDimLabel()
    {
        if(dimLabel == null)
        {
            dimLabel = new JLabel("Dimension");
        }
        return dimLabel;
    }

    public JComboBox<Integer> getDimComboBox()
    {
        if(dimComboBox == null)
        {
            dimComboBox = new JComboBox<Integer>();
            dimComboBox.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if(e.getActionCommand().equals("comboBoxChanged"))
                    {
                        if(dimComboBox.getSelectedItem() != null)
                            dimension = (Integer) dimComboBox.getSelectedItem();
                    }
                }
            });
        }
        return dimComboBox;
    }

    public class DisplayArea extends JPanel implements MouseListener, MouseMotionListener
    {
        int mouseClickedX;
        int centerClickedX;
        int mouseClickedY;
        int centerClickedZ;

        Point center;

        @Override
        public void paint(Graphics g1)
        {
            synchronized(dimensionChunks)
            {
                Graphics2D g = (Graphics2D) g1;
                Dimension dim = getSize();
                g.clearRect(0, 0, dim.width, dim.height);

                center = new Point(dim.width / 2, dim.height / 2);
                DimensionChunkInfo dimInfo = dimensionChunks.get(dimension);
                if(dimInfo == null)
                {
                    dimension = 0;
                    return;
                }

                g.setColor(new Color(1F, 0, 0));
                for(ChunkCoordIntPair coord : dimInfo.allchunks)
                {
                    Point pos = getChunkRenderPosition(coord.chunkXPos, coord.chunkZPos);
                    g.fillRect(pos.x, pos.y, 4, 4);
                }

                HashSet<ChunkCoordIntPair> forcedChunks = new HashSet<ChunkCoordIntPair>();
                int numTickets = 0;
                for(TicketInfo ticket : dimInfo.tickets.values())
                {
                    if(!ticket.chunkSet.isEmpty())
                        numTickets++;
                    for(ChunkCoordIntPair coord : ticket.chunkSet)
                        forcedChunks.add(coord);
                }

                g.setColor(new Color(0, 1F, 0));
                for(ChunkCoordIntPair coord : forcedChunks)
                {
                    Point pos = getChunkRenderPosition(coord.chunkXPos, coord.chunkZPos);
                    g.fillRect(pos.x + 1, pos.y + 1, 2, 2);
                }

                int numPlayers = 0;
                g.setColor(new Color(0, 0, 1F));
                for(PlayerInfo info : players.values())
                {
                    if(info.dimension == dimension)
                    {
                        Point pos = getChunkRenderPosition((int) info.position.x, 0, (int) info.position.z);
                        g.fillRect(pos.x + 1, pos.y + 1, 2, 2);
                        numPlayers++;
                    }
                }

                g.setColor(new Color(0, 0, 0));
                for(int x = (xCenter >> 4) - (center.x >> 2) - 2; x < (xCenter >> 4) + (center.x >> 2) + 2; x++)
                {
                    if(x % 16 == 0)
                    {
                        Point pos = getChunkRenderPosition(x, ((zCenter + 128) >> 8) << 4);
                        g.drawLine(pos.x, 0, pos.x, dim.height);

                        g.drawString(Integer.toString(x << 4), pos.x + 2, pos.y + 12);
                    }
                }
                for(int z = (zCenter >> 4) - (center.y >> 2) - 2; z < (zCenter >> 4) + (center.y >> 2) + 2; z++)
                {
                    if(z % 16 == 0)
                    {
                        Point pos = getChunkRenderPosition(((xCenter + 128) >> 8) << 4, z);
                        g.drawLine(0, pos.y, dim.width, pos.y);

                        g.drawString(Integer.toString(z << 4), pos.x + 2, pos.y - 2);
                    }
                }

                g.setColor(new Color(1F, 1F, 1F));
                g.fillRect(0, 0, 100, 60);

                g.setColor(new Color(0, 0, 0));
                g.drawString("Tickets: " + numTickets, 10, 20);
                g.drawString("Forced Chunks: " + forcedChunks.size(), 10, 30);
                g.drawString("Chunks: " + dimInfo.allchunks.size(), 10, 40);
                g.drawString("Players: " + numPlayers, 10, 50);
            }
        }

        public Point getChunkRenderPosition(int chunkX, int chunkZ)
        {
            int relBlockX = (chunkX << 4) + 8 - xCenter;
            int relBlockZ = (chunkZ << 4) + 8 - zCenter;
            return new Point(center.x + (relBlockX >> 2), center.y + (relBlockZ >> 2));
        }

        public Point getChunkRenderPosition(int blockX, int blockY, int blockZ)
        {
            return getChunkRenderPosition(blockX >> 4, blockZ >> 4);
        }

        @SuppressWarnings("unused")
        @Override
        public void mouseClicked(MouseEvent event)
        {
            if(event.getButton() != MouseEvent.BUTTON1 || event.getClickCount() < 2)
                return;

            Point mouse = event.getPoint();
            DimensionChunkInfo dimInfo = dimensionChunks.get(dimension);
            if(dimInfo == null)
            {
                dimension = 0;
                return;
            }

            LinkedList<TicketInfo> mouseOverTickets = getTicketsUnderMouse(dimInfo, mouse);
            if(!mouseOverTickets.isEmpty())
                new TicketInfoDialog(mouseOverTickets);
        }

        @Override
        public void mousePressed(MouseEvent event)
        {
            mouseClickedX = event.getX();
            centerClickedX = xCenter;
            mouseClickedY = event.getY();
            centerClickedZ = zCenter;
        }

        @Override
        public void mouseReleased(MouseEvent event)
        {
        }

        @Override
        public void mouseEntered(MouseEvent event)
        {
        }

        @Override
        public void mouseExited(MouseEvent event)
        {
        }

        @Override
        public void mouseDragged(MouseEvent event)
        {
            setCenter((mouseClickedX - event.getX()) * 4 + centerClickedX, (mouseClickedY - event.getY()) * 4 + centerClickedZ);
        }

        public LinkedList<TicketInfo> getTicketsUnderMouse(DimensionChunkInfo dimInfo, Point mouse)
        {
            LinkedList<TicketInfo> mouseOverTickets = new LinkedList<TicketInfo>();
            for(TicketInfo ticket : dimInfo.tickets.values())
            {
                for(ChunkCoordIntPair coord : ticket.chunkSet)
                {
                    Point pos = getChunkRenderPosition(coord.chunkXPos, coord.chunkZPos);
                    if(new Rectangle(pos.x, pos.y, 4, 4).contains(mouse))
                    {
                        mouseOverTickets.add(ticket);
                    }
                }
            }
            return mouseOverTickets;
        }

        @Override
        public void mouseMoved(MouseEvent event)
        {
            synchronized(dimensionChunks)
            {
                Point mouse = event.getPoint();
                DimensionChunkInfo dimInfo = dimensionChunks.get(dimension);
                if(dimInfo == null)
                {
                    dimension = 0;
                    return;
                }
                String tip = "";
                LinkedList<TicketInfo> mouseOverTickets = getTicketsUnderMouse(dimInfo, mouse);
                if(!mouseOverTickets.isEmpty())
                {
                    tip += mouseOverTickets.size() + (mouseOverTickets.size() == 1 ? " ticket" : " tickets");
                    for(TicketInfo info : mouseOverTickets)
                    {
                        tip += "\n" + info.modId;
                        if(info.player != null)
                            tip += ", " + info.player;
                    }
                }

                for(PlayerInfo info : players.values())
                {
                    if(info.dimension == dimension)
                    {
                        Point pos = getChunkRenderPosition((int) info.position.x, 0, (int) info.position.z);
                        if(new Rectangle(pos.x, pos.y, 4, 4).contains(mouse))
                            tip += "\n\n"+info.username + "\n(" + String.format("%.2f", info.position.x) + ", " + String.format("%.2f", info.position.y) + ", " + String.format("%.2f", info.position.z) + ")";
                    }
                }
                setToolTipText(tip.length() > 0 ? tip : null);
            }
        }

        @Override
        public void setToolTipText(String paramString)
        {
            if(paramString == null)
                super.setToolTipText(null);
            else
                super.setToolTipText("<html>" + paramString.replace("\n", "<br>") + "</html>");
        }
    }

    private static PlayerChunkViewer instance;

    public static PlayerChunkViewer instance()
    {
        return instance;
    }

    public void setCenter(int blockX, int blockZ)
    {
        xArea.setText(Integer.toString(blockX));
        xCenter = blockX;
        zArea.setText(Integer.toString(blockZ));
        zCenter = blockZ;
    }

    public void loadDimension(PacketCustom packet, WorldClient world)
    {
        synchronized(dimensionChunks)
        {
            DimensionChunkInfo dimInfo = new DimensionChunkInfo(packet.readInt());

            int numChunks = packet.readInt();
            for(int i = 0; i < numChunks; i++)
                dimInfo.allchunks.add(new ChunkCoordIntPair(packet.readInt(), packet.readInt()));

            int numTickets = packet.readInt();
            for(int i = 0; i < numTickets; i++)
            {
                TicketInfo ticket = new TicketInfo(packet, world);
                dimInfo.tickets.put(ticket.ID, ticket);
            }

            dimensionChunks.put(dimInfo.dimension, dimInfo);
        }
    }

    public void unloadDimension(int dim)
    {
        dimensionChunks.remove(dim);
    }

    public void handleChunkChange(int dimension, ChunkCoordIntPair coord, boolean add)
    {
        synchronized(dimensionChunks)
        {
            if(add)
                dimensionChunks.get(dimension).allchunks.add(coord);
            else
                dimensionChunks.get(dimension).allchunks.remove(coord);
        }
    }

    public void handleTicketChange(int dimension, int ticketID, ChunkCoordIntPair coord, boolean force)
    {
        synchronized(dimensionChunks)
        {
            DimensionChunkInfo dimInfo = dimensionChunks.get(dimension);
            TicketInfo ticket = dimInfo.tickets.get(ticketID);
            if(force)
                ticket.chunkSet.add(coord);
            else
                ticket.chunkSet.remove(coord);
        }
    }

    public void handleNewTicket(PacketCustom packet, WorldClient world)
    {
        synchronized(dimensionChunks)
        {
            int dim = packet.readInt();
            TicketInfo ticket = new TicketInfo(packet, world);
            dimensionChunks.get(dim).tickets.put(ticket.ID, ticket);
        }
    }

    public void handlePlayerUpdate(String username, int dimension, Vector3 position)
    {
        synchronized(dimensionChunks)
        {
            PlayerInfo info = players.get(username);
            if(info == null)
                players.put(username, info = new PlayerInfo(username));
            info.dimension = dimension;
            info.position = position;
        }
    }

    public void removePlayer(String username)
    {
        synchronized(dimensionChunks)
        {
            players.remove(username);
        }
    }
}
