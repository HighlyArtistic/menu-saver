package com.menusaver;

import net.runelite.api.MenuEntry;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MenuSaverPanel extends PluginPanel
{
    private final MenuSaverPlugin plugin;
    private final MenuSaverConfig config;
    private final JPanel groupsPanel = new JPanel();
    private final List<GroupPanel> groups = new ArrayList<>();
    private GroupPanel expandedGroup = null;
    private int groupCount = 0;

    public MenuSaverPanel(MenuSaverPlugin plugin, MenuSaverConfig config)
    {
        this.plugin = plugin;
        this.config = config;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title
        JLabel title = new JLabel("Menu Saver");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setBorder(new EmptyBorder(0, 0, 10, 0));

        // New Group button
        JButton newGroupButton = new JButton("+ New Group");
        newGroupButton.addActionListener(e -> createNewGroup());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(newGroupButton, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Groups container
        groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));
        groupsPanel.setBackground(getBackground());

        JScrollPane scrollPane = new JScrollPane(groupsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Reminder label shown when no group is expanded
        updateReminderLabel();
    }

    // Called from plugin when Save Menu is clicked in game
    public void saveCurrentMenu(List<MenuEntry> entries)
    {
        SwingUtilities.invokeLater(() ->
        {
            if (expandedGroup == null)
            {
                if (config.showGroupReminders())
                {
                    JOptionPane.showMessageDialog(
                            this,
                            "Please open a group in the Menu Saver panel first!",
                            "No Group Selected",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
                return;
            }
            expandedGroup.addMenuCard(entries);
        });
    }

    private void createNewGroup()
    {
        groupCount++;
        String defaultName = "Group " + groupCount;
        GroupPanel group = new GroupPanel(defaultName, this);
        groups.add(group);
        groupsPanel.add(group);
        groupsPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        groupsPanel.revalidate();
        groupsPanel.repaint();

        // Auto expand the newly created group
        setExpandedGroup(group);
    }

    public void setExpandedGroup(GroupPanel group)
    {
        // Collapse current group if there is one
        if (expandedGroup != null && expandedGroup != group)
        {
            expandedGroup.setExpanded(false);
        }
        expandedGroup = group;
        group.setExpanded(true);
        updateReminderLabel();
    }

    public void removeGroup(GroupPanel group)
    {
        if (expandedGroup == group)
        {
            expandedGroup = null;
        }
        groups.remove(group);

        // Remove the group and its spacing from the panel
        Component[] components = groupsPanel.getComponents();
        for (int i = 0; i < components.length; i++)
        {
            if (components[i] == group && i + 1 < components.length)
            {
                groupsPanel.remove(components[i + 1]);
                break;
            }
        }
        groupsPanel.remove(group);
        groupsPanel.revalidate();
        groupsPanel.repaint();
        updateReminderLabel();
    }

    public void notifyGroupCollapsed(GroupPanel group)
    {
        if (expandedGroup == group)
        {
            expandedGroup = null;
        }
        updateReminderLabel();
    }

    private void updateReminderLabel()
    {
        // Nothing needed here for now - reminder is shown as a popup
    }

    // ─── Inner class: one group in the panel ───────────────────────────────

    static class GroupPanel extends JPanel
    {
        private final MenuSaverPanel parent;
        private final JPanel cardsPanel = new JPanel();
        private final JLabel titleLabel;
        private boolean expanded = false;
        private int cardCount = 0;

        GroupPanel(String name, MenuSaverPanel parent)
        {
            this.parent = parent;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            setBorder(new LineBorder(Color.GRAY, 1));
            setBackground(new Color(40, 40, 40));

            // Header row
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(60, 60, 60));
            header.setBorder(new EmptyBorder(5, 8, 5, 8));

            titleLabel = new JLabel("▶ " + name);
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
            titleLabel.setToolTipText("Click to expand/collapse. Double-click to rename.");

            // Single click — expand/collapse
            titleLabel.addMouseListener(new java.awt.event.MouseAdapter()
            {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e)
                {
                    if (e.getClickCount() == 1)
                    {
                        if (expanded)
                        {
                            setExpanded(false);
                            parent.notifyGroupCollapsed(GroupPanel.this);
                        }
                        else
                        {
                            parent.setExpandedGroup(GroupPanel.this);
                        }
                    }
                    else if (e.getClickCount() == 2)
                    {
                        renameGroup();
                    }
                }
            });

            // Delete group button
            JButton deleteButton = new JButton("X");
            deleteButton.setPreferredSize(new Dimension(30, 20));
            deleteButton.setForeground(Color.RED);
            deleteButton.setToolTipText("Delete this group");
            deleteButton.addActionListener(e ->
            {
                int confirm = JOptionPane.showConfirmDialog(
                        parent,
                        "Delete group \"" + titleLabel.getText().replace("▶ ", "").replace("▼ ", "") + "\" and all its saved menus?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION)
                {
                    parent.removeGroup(GroupPanel.this);
                }
            });

            header.add(titleLabel, BorderLayout.CENTER);
            header.add(deleteButton, BorderLayout.EAST);
            add(header);

            // Cards container — hidden until expanded
            cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
            cardsPanel.setBackground(new Color(40, 40, 40));
            cardsPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
            cardsPanel.setVisible(false);
            add(cardsPanel);
        }

        void setExpanded(boolean expand)
        {
            this.expanded = expand;
            cardsPanel.setVisible(expand);
            String rawName = titleLabel.getText().replace("▶ ", "").replace("▼ ", "");
            titleLabel.setText((expand ? "▼ " : "▶ ") + rawName);
            revalidate();
            repaint();
        }

        void renameGroup()
        {
            String rawName = titleLabel.getText().replace("▶ ", "").replace("▼ ", "");
            String newName = JOptionPane.showInputDialog(parent, "Enter new group name:", rawName);
            if (newName != null && !newName.trim().isEmpty())
            {
                titleLabel.setText((expanded ? "▼ " : "▶ ") + newName.trim());
            }
        }

        void addMenuCard(List<MenuEntry> entries)
        {
            cardCount++;
            String defaultTitle = "Menu " + cardCount;
            MenuCardPanel card = new MenuCardPanel(defaultTitle, entries, parent.plugin, this);
            cardsPanel.add(card);
            cardsPanel.add(Box.createRigidArea(new Dimension(0, 6)));
            cardsPanel.revalidate();
            cardsPanel.repaint();
        }

        void removeCard(MenuCardPanel card)
        {
            Component[] components = cardsPanel.getComponents();
            for (int i = 0; i < components.length; i++)
            {
                if (components[i] == card && i + 1 < components.length)
                {
                    cardsPanel.remove(components[i + 1]);
                    break;
                }
            }
            cardsPanel.remove(card);
            cardsPanel.revalidate();
            cardsPanel.repaint();
        }
    }

    // ─── Inner class: one saved menu card ──────────────────────────────────

    static class MenuCardPanel extends JPanel
    {
        MenuCardPanel(String defaultTitle, List<MenuEntry> entries, MenuSaverPlugin plugin, GroupPanel group)
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new LineBorder(new Color(80, 80, 80), 1));
            setBackground(new Color(50, 50, 50));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            // Card header
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(70, 70, 70));
            header.setBorder(new EmptyBorder(4, 6, 4, 6));

            JLabel titleLabel = new JLabel(defaultTitle);
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
            titleLabel.setToolTipText("Double-click to rename");

            // Double click to rename card
            titleLabel.addMouseListener(new java.awt.event.MouseAdapter()
            {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e)
                {
                    if (e.getClickCount() == 2)
                    {
                        String newName = JOptionPane.showInputDialog(
                                group.parent,
                                "Enter new menu name:",
                                titleLabel.getText()
                        );
                        if (newName != null && !newName.trim().isEmpty())
                        {
                            titleLabel.setText(newName.trim());
                        }
                    }
                }
            });

            // Delete card button
            JButton deleteButton = new JButton("X");
            deleteButton.setPreferredSize(new Dimension(26, 18));
            deleteButton.setForeground(Color.RED);
            deleteButton.setToolTipText("Remove this saved menu");
            deleteButton.addActionListener(e -> group.removeCard(MenuCardPanel.this));

            header.add(titleLabel, BorderLayout.CENTER);
            header.add(deleteButton, BorderLayout.EAST);
            add(header);

            // Divider
            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(80, 80, 80));
            add(sep);

            // One button per entry
            for (MenuEntry entry : entries)
            {
                String rawTarget = entry.getTarget();
                String cleanTarget = rawTarget.replaceAll("<[^>]*>", "").trim();
                String label = cleanTarget.isEmpty()
                        ? entry.getOption()
                        : entry.getOption() + " " + cleanTarget;

                JButton actionButton = new JButton(label);
                actionButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                actionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                actionButton.setHorizontalAlignment(SwingConstants.LEFT);
                actionButton.setBorder(new EmptyBorder(3, 8, 3, 8));
                actionButton.setToolTipText("Click to perform this action in game");
                actionButton.addActionListener(e -> plugin.invokeMenuEntry(entry));
                add(actionButton);
            }
        }
    }
}