import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class Main implements Runnable{
    private SystemTray tray;
    private TrayIcon trayIcon;
    private int lastIcon;
    private MenuItem pingLabel;
    private boolean offline;
    private JFrame pingWindow;

    public static void main(String[] args){
        new Main();
    }

    public Main(){
        if (!SystemTray.isSupported()) {
            System.err.println("SystemTray is not supported");
            return;
        }
        pingWindow = new JFrame("Ping status");

        PopupMenu popup = new PopupMenu();
        trayIcon = new TrayIcon(getImage("icons.png", 0));
        lastIcon = 0;
        tray = SystemTray.getSystemTray();

        // Create a pop-up menu components
        pingLabel = new MenuItem("Ping: " + 0);
        CheckboxMenuItem showWindow = new CheckboxMenuItem("Show ping window");
        showWindow.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {

            }
        });
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tray.remove(trayIcon);
                System.exit(0);
            }
        });

        //Add components to pop-up menu
        popup.add(pingLabel);
        popup.addSeparator();
        popup.add(showWindow);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("TrayIcon could not be added.");
        }

        Thread t = new Thread(this);
        t.run();
    }

    @Override
    public void run() {
        int i = 0;
        int avgPing = 0;
        final int avgPingMax = 10;
        int pingInterval = 100;
        offline = false;
        while(true){
            try {
                Thread.sleep(pingInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int google = ping("8.8.8.8");
            int facebook = ping("31.13.72.8");
            int reddit = ping("151.101.193.140");
            int ping = ((google > reddit + facebook + 100 ? 5 : google) +(reddit > google + facebook + 100 ? 5 : reddit) + (facebook > reddit + google + 100 ? 5 : facebook))/3;
            if(ping > 1000 && offline == false) {
                JOptionPane.showMessageDialog(null, "no network connection");
                offline = true;
            }else
                offline = false;
            avgPing += ping;
            i++;
            if(i >= avgPingMax){
                avgPing /= avgPingMax;
                changeIcon(avgPing);
                pingLabel.setLabel("Ping: " + avgPing);
                avgPing = 0;
                i = 0;
            }
        }
    }

    private void changeIcon(int ping){
        int icon = 0;
        if(ping <= 5)
            icon = 5;
        else if(ping <= 10)
            icon = 4;
        else if(ping <= 20)
            icon = 3;
        else if(ping <= 40)
            icon = 2;
        else if(ping <= 80)
            icon = 1;
        else if(ping <= 1000)
            icon = 0;
        if(lastIcon != icon) {
            trayIcon.setImage(getImage("icons.png", icon));
        }
    }

    private int ping(String host) {
        try{
            int time = 1000000;
            String cmd = "ping -n 1 " + host;
            Process pingProcess = Runtime.getRuntime().exec(cmd);
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(pingProcess.getInputStream()));
            String s;
            int i = 0;
            while ((s = stdInput.readLine()) != null) {
                if(s.startsWith("Reply from ")) {
                    time = Integer.parseInt(s.substring(s.indexOf("time") + 5, s.indexOf("ms")));
                    break;
                }
                i++;
            }
            pingProcess.waitFor(1000, TimeUnit.MILLISECONDS);
            if(pingProcess.exitValue() == 0) {
                return time;
            } else {

                return 1000000;
            }

        } catch( Exception e ) {

            e.printStackTrace();
            return 1000000;
        }
    }

    private static BufferedImage getImage(String name, int icon){
        try {
            return ImageIO.read(new File("src/icons.png")).getSubimage(icon * 16, 0, 16, 16);
        } catch (IOException e) {
            System.err.println("Failed to load image");
            return null;
        }
    }
}
