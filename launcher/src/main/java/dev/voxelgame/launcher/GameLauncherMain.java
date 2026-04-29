package dev.voxelgame.launcher;

import dev.voxelgame.client.ClientMain;
import dev.voxelgame.server.GameServerMain;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public final class GameLauncherMain {
    private GameLauncherMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameLauncherMain::showLauncher);
    }

    private static void showLauncher() {
        JFrame frame = new JFrame("Voxel Survival Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 360);
        frame.setLocationRelativeTo(null);

        JTextField seed = new JTextField("1337");
        JTextField renderDistance = new JTextField("8");
        JTextField previewRadius = new JTextField("3");
        JTextField host = new JTextField("127.0.0.1");
        JTextField port = new JTextField("25565");
        JTextField username = new JTextField("Player");
        JCheckBox autoServer = new JCheckBox("Start local server before joining");

        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Seed"));
        fields.add(seed);
        fields.add(new JLabel("Render distance"));
        fields.add(renderDistance);
        fields.add(new JLabel("Preview radius"));
        fields.add(previewRadius);
        fields.add(new JLabel("Server host"));
        fields.add(host);
        fields.add(new JLabel("Server port"));
        fields.add(port);
        fields.add(new JLabel("Username"));
        fields.add(username);
        fields.add(new JLabel(""));
        fields.add(autoServer);

        JButton singleplayer = new JButton("Play Singleplayer");
        JButton multiplayer = new JButton("Join Server");
        JButton server = new JButton("Start Server");
        JButton quit = new JButton("Quit");

        singleplayer.addActionListener(event -> launchClient(
                "--auto-singleplayer",
                "--seed", seed.getText(),
                "--preview-radius", previewRadius.getText(),
                "--render-distance", renderDistance.getText()
        ));
        multiplayer.addActionListener(event -> {
            if (autoServer.isSelected()) {
                launchServer(port.getText(), seed.getText());
            }
            launchClient(
                    "--auto-join",
                    "--connect", host.getText(),
                    "--port", port.getText(),
                    "--username", username.getText(),
                    "--render-distance", renderDistance.getText()
            );
        });
        server.addActionListener(event -> launchServer(port.getText(), seed.getText()));
        quit.addActionListener(event -> System.exit(0));

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.add(singleplayer);
        buttons.add(multiplayer);
        buttons.add(server);
        buttons.add(quit);

        frame.add(fields, BorderLayout.CENTER);
        frame.add(buttons, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static void launchClient(String... args) {
        Thread thread = new Thread(() -> ClientMain.main(args), "voxel-client");
        thread.setDaemon(false);
        thread.start();
    }

    private static void launchServer(String port, String seed) {
        Thread thread = new Thread(() -> {
            try {
                GameServerMain.main(new String[]{"--port", port, "--seed", seed, "--whitelist", "Player"});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "voxel-server");
        thread.setDaemon(true);
        thread.start();
    }
}
