import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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

    // Adjustable speeds (modified by game mode)
    private int playerSpeed = 5;
    private int obstacleSpeed = 3;
    private double obstacleSpawnRate = 0.02;

    private static final int PROJECTILE_SPEED = 10;

    private int score = 0;
    private int health = 100;
    private static final int MAX_HEALTH = 100;

    private Clip clip;

    private JPanel gamePanel;
    private JPanel menuPanel;
    private JLabel scoreLabel;
    private JLabel healthLabel;
    private JLabel timerLabel;
    private Timer timer;
    private boolean isGameOver;
    private boolean isGameWon;
    private int playerX, playerY;
    private int projectileX, projectileY;
    private boolean isProjectileVisible;
    private boolean isFiring;
    private List<Point> obstacles;
    private List<Point> stars;
    private List<Point> powerups;

    private int spriteWidth = 64;
    private int spriteHeight = 64;

    private boolean shieldActive = false;
    private int shieldDuration = 5000;
    private long shieldStartTime;

    // Game timer (1 minute countdown)
    private long gameStartTime;
    private static final int GAME_DURATION_MS = 60000; // 60 seconds

    // Powerup spawning
    private long lastPowerupSpawnTime;
    private int nextPowerupSpawnDelay;
    private Random random = new Random();

    // Game mode
    private boolean isChallengeMode = false;

    private BufferedImage shipImage;
    private BufferedImage spriteSheet;

    private void deactivateShield() {
        shieldActive = false;
    }

    private boolean isShieldActive() {
        return shieldActive && (System.currentTimeMillis() - shieldStartTime) < shieldDuration;
    }

    private void activateShield() {
        shieldActive = true;
        shieldStartTime = System.currentTimeMillis();
    }

    public MainGame() {
        setTitle("Space Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Load resources
        try {
            shipImage = ImageIO.read(new File("spaceship160.png"));
            spriteSheet = ImageIO.read(new File("asteroid64.png"));
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    new File("334268__aceofspadesproduc100__launching-1.wav"));
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        createMenuPanel();
        createGamePanel();

        add(menuPanel);
        setVisible(true);
    }

    private void createMenuPanel() {
        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(Color.BLACK);

        JLabel titleLabel = new JLabel("SPACE GAME");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.CYAN);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton normalButton = new JButton("Normal Mode");
        normalButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        normalButton.setFont(new Font("Arial", Font.BOLD, 18));
        normalButton.addActionListener(e -> startGame(false));

        JButton challengeButton = new JButton("Challenge Mode");
        challengeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        challengeButton.setFont(new Font("Arial", Font.BOLD, 18));
        challengeButton.addActionListener(e -> startGame(true));

        menuPanel.add(Box.createVerticalGlue());
        menuPanel.add(titleLabel);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 50)));
        menuPanel.add(normalButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        menuPanel.add(challengeButton);
        menuPanel.add(Box.createVerticalGlue());
    }

    private void createGamePanel() {
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };
        gamePanel.setLayout(null);

        stars = generateStars(200);

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setBounds(10, 10, 150, 25);
        scoreLabel.setForeground(Color.CYAN);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gamePanel.add(scoreLabel);

        healthLabel = new JLabel("Health: 100");
        healthLabel.setBounds(10, 35, 150, 25);
        healthLabel.setForeground(Color.GREEN);
        healthLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gamePanel.add(healthLabel);

        timerLabel = new JLabel("Time: 60");
        timerLabel.setBounds(WIDTH - 110, 10, 100, 25);
        timerLabel.setForeground(Color.YELLOW);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gamePanel.add(timerLabel);

        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);
    }

    private void startGame(boolean challengeMode) {
        isChallengeMode = challengeMode;

        // Set difficulty parameters
        if (isChallengeMode) {
            playerSpeed = 8;
            obstacleSpeed = 5;
            obstacleSpawnRate = 0.05;
        } else {
            playerSpeed = 5;
            obstacleSpeed = 3;
            obstacleSpawnRate = 0.02;
        }

        // Initialize game state
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 35;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;
        isProjectileVisible = false;
        isGameOver = false;
        isGameWon = false;
        isFiring = false;
        score = 0;
        health = MAX_HEALTH;

        obstacles = new ArrayList<>();
        powerups = new ArrayList<>();

        gameStartTime = System.currentTimeMillis();
        lastPowerupSpawnTime = gameStartTime;
        nextPowerupSpawnDelay = getRandomPowerupDelay();

        // Switch panels
        remove(menuPanel);
        add(gamePanel);
        revalidate();
        repaint();
        gamePanel.requestFocusInWindow();

        // Start game loop
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(20, e -> {
            if (!isGameOver && !isGameWon) {
                update();
                gamePanel.repaint();
            }
        });
        timer.start();
    }

    private int getRandomPowerupDelay() {
        // Random delay between 10 and 15 seconds (in milliseconds)
        return 10000 + random.nextInt(5001);
    }

    private void draw(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw stars
        g.setColor(generateRandomColor());
        for (Point star : stars) {
            g.fillOval(star.x, star.y, 2, 2);
        }

        // Draw ship
        g.drawImage(shipImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);

        // Draw projectile
        if (isProjectileVisible) {
            g.setColor(Color.GREEN);
            g.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        }

        // Draw shield
        if (isShieldActive()) {
            g.setColor(new Color(0, 255, 255, 100));
            g.fillOval(playerX - 5, playerY - 5, PLAYER_WIDTH + 10, PLAYER_HEIGHT + 10);
        }

        // Draw obstacles (asteroids)
        for (Point obstacle : obstacles) {
            if (spriteSheet != null) {
                int spriteIndex = random.nextInt(4);
                int spriteX = spriteIndex * spriteWidth;
                int spriteY = 0;
                g.drawImage(spriteSheet.getSubimage(spriteX, spriteY, spriteWidth, spriteHeight),
                        obstacle.x, obstacle.y, null);
            }
        }

        // Draw powerups (health packs)
        g.setColor(Color.MAGENTA);
        for (Point powerup : powerups) {
            g.fillOval(powerup.x, powerup.y, 20, 20);
            g.setColor(Color.WHITE);
            g.drawString("+", powerup.x + 6, powerup.y + 15);
            g.setColor(Color.MAGENTA);
        }

        // Draw health bar
        int barWidth = 100;
        int barHeight = 10;
        int barX = 10;
        int barY = 60;
        g.setColor(Color.DARK_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(health > 50 ? Color.GREEN : (health > 25 ? Color.YELLOW : Color.RED));
        g.fillRect(barX, barY, (int) (barWidth * (health / 100.0)), barHeight);
        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, barWidth, barHeight);

        // Draw game over or win message
        if (isGameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("GAME OVER", WIDTH / 2 - 90, HEIGHT / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.setColor(Color.WHITE);
            g.drawString("Press ESC to return to menu", WIDTH / 2 - 100, HEIGHT / 2 + 30);
        } else if (isGameWon) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("YOU WIN!", WIDTH / 2 - 70, HEIGHT / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.setColor(Color.WHITE);
            g.drawString("Press ESC to return to menu", WIDTH / 2 - 100, HEIGHT / 2 + 30);
        }
    }

    private void update() {
        long currentTime = System.currentTimeMillis();

        // Update countdown timer
        long elapsedTime = currentTime - gameStartTime;
        int remainingSeconds = (int) ((GAME_DURATION_MS - elapsedTime) / 1000);

        if (remainingSeconds <= 0) {
            isGameWon = true;
            timer.stop();
            return;
        }

        timerLabel.setText("Time: " + remainingSeconds);

        // Move obstacles
        for (int i = 0; i < obstacles.size(); i++) {
            obstacles.get(i).y += obstacleSpeed;
            if (obstacles.get(i).y > HEIGHT) {
                obstacles.remove(i);
                i--;
            }
        }

        // Generate new obstacles
        if (Math.random() < obstacleSpawnRate) {
            int obstacleX = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH));
            obstacles.add(new Point(obstacleX, 0));
        }

        // Spawn powerups (10-15 second intervals)
        if (currentTime - lastPowerupSpawnTime >= nextPowerupSpawnDelay) {
            int powerupX = random.nextInt(WIDTH - 20);
            int powerupY = random.nextInt(HEIGHT / 2); // Spawn in upper half
            powerups.add(new Point(powerupX, powerupY));
            lastPowerupSpawnTime = currentTime;
            nextPowerupSpawnDelay = getRandomPowerupDelay();
        }

        // Move powerups down slowly
        for (int i = 0; i < powerups.size(); i++) {
            powerups.get(i).y += 1;
            if (powerups.get(i).y > HEIGHT) {
                powerups.remove(i);
                i--;
            }
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

        // Player vs obstacles
        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle obstacleRect = new Rectangle(obstacles.get(i).x, obstacles.get(i).y,
                    OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            if (playerRect.intersects(obstacleRect)) {
                if (!isShieldActive()) {
                    health -= 25;
                    healthLabel.setText("Health: " + health);
                    if (health <= 0) {
                        health = 0;
                        isGameOver = true;
                        timer.stop();
                    }
                }
                obstacles.remove(i);
                i--;
            }
        }

        // Player vs powerups
        for (int i = 0; i < powerups.size(); i++) {
            Rectangle powerupRect = new Rectangle(powerups.get(i).x, powerups.get(i).y, 20, 20);
            if (playerRect.intersects(powerupRect)) {
                health = Math.min(health + 25, MAX_HEALTH);
                healthLabel.setText("Health: " + health);
                powerups.remove(i);
                i--;
            }
        }

        // Projectile vs obstacles
        Rectangle projectileRect = new Rectangle(projectileX, projectileY,
                PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle obstacleRect = new Rectangle(obstacles.get(i).x, obstacles.get(i).y,
                    OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            if (projectileRect.intersects(obstacleRect) && isProjectileVisible) {
                obstacles.remove(i);
                score += 10;
                isProjectileVisible = false;
                break;
            }
        }

        scoreLabel.setText("Score: " + score);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_ESCAPE) {
            returnToMenu();
            return;
        }

        if (isGameOver || isGameWon) {
            return;
        }

        if (keyCode == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= playerSpeed;
        } else if (keyCode == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH) {
            playerX += playerSpeed;
        } else if (keyCode == KeyEvent.VK_CONTROL) {
            activateShield();
        } else if (keyCode == KeyEvent.VK_SPACE && !isFiring) {
            playsound();
            isFiring = true;
            projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
            projectileY = playerY;
            isProjectileVisible = true;
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    isFiring = false;
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    private void returnToMenu() {
        if (timer != null) {
            timer.stop();
        }
        remove(gamePanel);
        add(menuPanel);
        revalidate();
        repaint();
    }

    private List<Point> generateStars(int numStars) {
        List<Point> starsList = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < numStars; i++) {
            int x = rand.nextInt(WIDTH);
            int y = rand.nextInt(HEIGHT);
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

    public void playsound() {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGame());
    }
}
