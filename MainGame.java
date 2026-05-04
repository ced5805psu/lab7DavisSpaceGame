import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.*;


public class MainGame extends JFrame implements KeyListener {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 50;
    private static final int OBSTACLE_WIDTH = 20;
    private static final int OBSTACLE_HEIGHT = 20;
    private static final int PROJECTILE_WIDTH = 5;
    private static final int PROJECTILE_HEIGHT = 10;
    private static final int PLAYER_SPEED = 5;
    private static final int OBSTACLE_SPEED = 3;
    private static final int PROJECTILE_SPEED = 10;
    private int score = 0;
    private Clip clip;


    private JPanel gamePanel;
    private JLabel scoreLabel;
    private Timer timer;
    private boolean isGameOver;
    private int playerX, playerY;
    private int projectileX, projectileY;
    private boolean isProjectileVisible;
    private boolean isFiring;
    private java.util.List<Point> obstacles;
    private List<Point> stars;

    private BufferedImage shipImage;
    private int currentFrame = 0;
    private static final int SCALE = 4;
    private static final int COLUMNS = 2;


    public MainGame() {
        setTitle("Space Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };

        // Spaceship png code
        try {
            // Load the ship image from file
            shipImage = ImageIO.read(new File("spaceship160.png"));
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("334268__aceofspadesproduc100__launching-1.wav"));
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
        } catch ( LineUnavailableException ex) {
            ex.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        stars = generateStars(200); // Generate 200 starts initially

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setBounds(10, 10, 100, 20);
        gamePanel.add(scoreLabel);
        scoreLabel.setForeground(Color.BLUE);
        scoreLabel.setFont(new Font("Comic Sans", Font.BOLD, 30));

        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 35;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;
        isProjectileVisible = false;
        isGameOver = false;
        isFiring = false;
        obstacles = new java.util.ArrayList<>();

        timer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isGameOver) {
                    update();
                    gamePanel.repaint();
                }
            }
        });
        timer.start();
    }

    private void draw(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // g.setColor(Color.BLUE);
        // g.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        // Drawing the ship sprite
        g.drawImage(shipImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);

        // Draw stars
        g.setColor(generateRandomColor());
        // g.setColor(Color.WHITE);
        for (Point star : stars) {
            g.fillOval(star.x, star.y, 2, 2);
        }

        // Drawing the sprite


        if (isProjectileVisible) {
            g.setColor(Color.GREEN);
            g.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        }

        g.setColor(Color.RED);
        for (Point obstacle : obstacles) {
            g.fillRect(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
        }

        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Game Over!", WIDTH / 2 - 80, HEIGHT / 2);
        }
    }

    private void update() {
        if (!isGameOver) {
            // Move obstacles
            for (int i = 0; i < obstacles.size(); i++) {
                obstacles.get(i).y += OBSTACLE_SPEED;
                if (obstacles.get(i).y > HEIGHT) {
                    obstacles.remove(i);
                    i--;
                }
            }

            // Generate new obstacles
            if (Math.random() < 0.02) {
                int obstacleX = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH));
                obstacles.add(new Point(obstacleX, 0));
            }

            // Move projectile
            if (isProjectileVisible) {
                projectileY -= PROJECTILE_SPEED;
                if (projectileY < 0) {
                    isProjectileVisible = false;
                }
            }

            // Check collision with player
            Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
            for (Point obstacle : obstacles) {
                Rectangle obstacleRect = new Rectangle(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                if (playerRect.intersects(obstacleRect)) {
                    isGameOver = true;
                    break;
                }
            }

            // Check collision with obstacle
            Rectangle projectileRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
            for (int i = 0; i < obstacles.size(); i++) {
                Rectangle obstacleRect = new Rectangle(obstacles.get(i).x, obstacles.get(i).y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                if (projectileRect.intersects(obstacleRect)) {
                    obstacles.remove(i);
                    score += 10;
                    isProjectileVisible = false;
                    break;
                }
            }

            scoreLabel.setText("Score: " + score);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH) {
            playerX += PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_SPACE && !isFiring) {
            isFiring = true;
            projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
            projectileY = playerY;
            isProjectileVisible = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500); // Limit firing rate
                        isFiring = false;
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainGame().setVisible(true);
            }
        });
    }
    private List<Point> generateStars(int numStars) {
        List<Point> starsList = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numStars; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            starsList.add(new Point(x, y));
        }
        return starsList;
    }

    public static Color generateRandomColor() {
        Random rand = new Random();
        int r = rand.nextInt(256);
        int g = rand.nextInt(256);
        int b = rand.nextInt(256);
        return new Color(r, g, b);
    }
}

