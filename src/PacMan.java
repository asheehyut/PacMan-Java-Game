import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;

public class PacMan extends JPanel implements ActionListener, KeyListener {

    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; // U D L R
        int velocityX = 0;
        int velocityY = 0;

        // r, o, p, b for ghosts, ' ' for others
        char ghostType = ' ';

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();

            // try move one step in that direction
            this.x += this.velocityX;
            this.y += this.velocityY;

            // revert if we hit a wall
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                    break;
                }
            }
        }

        void updateVelocity() {
            int speed = tileSize / 4;
            switch (this.direction) {
                case 'U' -> { this.velocityX = 0; this.velocityY = -speed; }
                case 'D' -> { this.velocityX = 0; this.velocityY =  speed; }
                case 'L' -> { this.velocityX = -speed; this.velocityY = 0; }
                case 'R' -> { this.velocityX =  speed; this.velocityY = 0; }
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

    // queued input: what the player wants to do next
    private char queuedDirection = '\0';

    // X = wall, O = skip, P = pac man, ' ' = food
    // b/o/p/r ghosts
    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "O       bpo       O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX"
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    Block pacman;

    Timer gameLoop;
    char[] directions = { 'U', 'D', 'L', 'R' };
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;

    public PacMan() {
        initializeGame();
    }

    private void initializeGame() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        loadImages();
        loadMap();
        initializeGhosts();
        startGameLoop();
    }

    private void loadImages() {
        wallImage        = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage   = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage   = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage    = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();

        pacmanUpImage    = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImage  = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImage  = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();
    }

    private void initializeGhosts() {
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
    }

    private void startGameLoop() {
        gameLoop = new Timer(50, this); // ~20 FPS
        gameLoop.start();
    }

    public void loadMap() {
        walls  = new HashSet<>();
        foods  = new HashSet<>();
        ghosts = new HashSet<>();

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char tile = row.charAt(c);

                int x = c * tileSize;
                int y = r * tileSize;

                if (tile == 'X') {              // wall
                    Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                    walls.add(wall);
                } else if (tile == 'b') {       // blue ghost
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = 'b';
                    ghosts.add(ghost);
                } else if (tile == 'o') {       // orange ghost
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = 'o';
                    ghosts.add(ghost);
                } else if (tile == 'p') {       // pink ghost
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = 'p';
                    ghosts.add(ghost);
                } else if (tile == 'r') {       // red ghost
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = 'r';
                    ghosts.add(ghost);
                } else if (tile == 'P') {       // pacman
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                } else if (tile == ' ') {       // food
                    Block food = new Block(null, x + 14, y + 14, 4, 4);
                    foods.add(food);
                }
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        drawPacman(g);
        drawGhosts(g);
        drawWalls(g);
        drawFoods(g);
        drawScore(g);
    }

    private void drawPacman(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);
    }

    private void drawGhosts(Graphics g) {
        for (Block ghost : ghosts) {
            g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }
    }

    private void drawWalls(Graphics g) {
        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }
    }

    private void drawFoods(Graphics g) {
        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height);
        }
    }

    private void drawScore(Graphics g) {
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver) {
            g.drawString("Game Over: " + score, tileSize / 2, tileSize / 2);
        } else {
            g.drawString("x" + lives + "  Score: " + score, tileSize / 2, tileSize / 2);
        }
    }

    // ========================= GAME LOGIC =========================

    public void move() {
        movePacman();
        moveGhosts();
        checkFoodCollision();

        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
        }
    }

    private void movePacman() {
        // try to apply queued turn before moving
        if (queuedDirection != '\0' && queuedDirection != pacman.direction) {
            char before = pacman.direction;
            pacman.updateDirection(queuedDirection);

            // clear queue if the turn actually worked
            if (pacman.direction == queuedDirection && before != pacman.direction) {
                updatePacmanImage();
                queuedDirection = '\0';
            }
        }

        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;
        checkWallCollision(pacman);
    }

    private void moveGhosts() {
        for (Block ghost : ghosts) {
            if (collision(ghost, pacman)) {
                handleGhostCollision();
                return;
            }

            switch (ghost.ghostType) {
                case 'r' -> moveRedGhost(ghost);
                case 'b' -> moveBlueGhost(ghost);
                case 'p' -> movePinkGhost(ghost);
                case 'o' -> moveOrangeGhost(ghost);
                default -> moveGenericGhost(ghost);
            }
        }
    }

    // ----- INDIVIDUAL GHOST AIs -----

    private void moveRedGhost(Block ghost) {
        // simple: chase Pac-Man directly
        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);

        // adjust heading towards Pac-Man when we can
        if (random.nextInt(5) == 0) {
            if (Math.abs(pacman.x - ghost.x) > Math.abs(pacman.y - ghost.y)) {
                if (pacman.x > ghost.x) ghost.updateDirection('R');
                else if (pacman.x < ghost.x) ghost.updateDirection('L');
            } else {
                if (pacman.y > ghost.y) ghost.updateDirection('D');
                else if (pacman.y < ghost.y) ghost.updateDirection('U');
            }
        }
    }

    private void moveBlueGhost(Block ghost) {
        // mirror-ish: sometimes head opposite to Pac-Man
        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);

        if (random.nextInt(10) == 0) {
            if (pacman.x > ghost.x) ghost.updateDirection('L');
            else if (pacman.x < ghost.x) ghost.updateDirection('R');
            else if (pacman.y > ghost.y) ghost.updateDirection('U');
            else if (pacman.y < ghost.y) ghost.updateDirection('D');
        }
    }

    private void movePinkGhost(Block ghost) {
        // ambush: aim a bit ahead of Pac-Man
        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);

        int lookAheadTiles = 2;
        int targetX = pacman.x + pacman.velocityX * lookAheadTiles;
        int targetY = pacman.y + pacman.velocityY * lookAheadTiles;

        if (random.nextInt(6) == 0) {
            if (Math.abs(targetX - ghost.x) > Math.abs(targetY - ghost.y)) {
                if (targetX > ghost.x) ghost.updateDirection('R');
                else if (targetX < ghost.x) ghost.updateDirection('L');
            } else {
                if (targetY > ghost.y) ghost.updateDirection('D');
                else if (targetY < ghost.y) ghost.updateDirection('U');
            }
        }
    }

    private void moveOrangeGhost(Block ghost) {
        // coward: flee when close, wander when far
        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);

        int dist = Math.abs(ghost.x - pacman.x) + Math.abs(ghost.y - pacman.y);
        int fleeRange = tileSize * 5;

        if (dist < fleeRange) {
            // flee from Pac-Man
            if (Math.abs(pacman.x - ghost.x) > Math.abs(pacman.y - ghost.y)) {
                if (pacman.x > ghost.x) ghost.updateDirection('L');
                else if (pacman.x < ghost.x) ghost.updateDirection('R');
            } else {
                if (pacman.y > ghost.y) ghost.updateDirection('U');
                else if (pacman.y < ghost.y) ghost.updateDirection('D');
            }
        } else {
            // random wandering
            if (random.nextInt(15) == 0) {
                ghost.updateDirection(directions[random.nextInt(4)]);
            }
        }
    }

    private void moveGenericGhost(Block ghost) {
        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);
    }

    // -------------------------------

    private void checkWallCollision(Block block) {
        for (Block wall : walls) {
            if (collision(block, wall) ||
                block.x < 0 ||
                block.x + block.width > boardWidth ||
                block.y < 0 ||
                block.y + block.height > boardHeight) {

                block.x -= block.velocityX;
                block.y -= block.velocityY;

                if (block != pacman) {
                    // random new direction for ghosts on collision
                    char newDirection = directions[random.nextInt(4)];
                    block.updateDirection(newDirection);
                }
                break;
            }
        }
    }

    private void handleGhostCollision() {
        lives--;
        if (lives <= 0) {
            gameOver = true;
        } else {
            resetPositions();
        }
    }

    private void checkFoodCollision() {
        Block eaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                eaten = food;
                score += 10;
            }
        }
        if (eaten != null) {
            foods.remove(eaten);
        }
    }

    public boolean collision(Block a, Block b) {
        return a.x < b.x + b.width &&
               a.x + a.width > b.x &&
               a.y < b.y + b.height &&
               a.y + a.height > b.y;
    }

    public void resetPositions() {
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        queuedDirection = '\0';

        for (Block ghost : ghosts) {
            ghost.reset();
            ghost.updateDirection(directions[random.nextInt(4)]);
        }
    }

    // ========================= EVENT HANDLERS =========================

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) {
            restartGame();
        } else {
            handleKeyPress(e);
        }
    }

    private void restartGame() {
        loadMap();
        resetPositions();
        lives = 3;
        score = 0;
        gameOver = false;
        gameLoop.start();
    }

    private void handleKeyPress(KeyEvent e) {
        // queue the direction – actual turn happens in movePacman()
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP    -> queuedDirection = 'U';
            case KeyEvent.VK_DOWN  -> queuedDirection = 'D';
            case KeyEvent.VK_LEFT  -> queuedDirection = 'L';
            case KeyEvent.VK_RIGHT -> queuedDirection = 'R';
        }
    }

    private void updatePacmanImage() {
        switch (pacman.direction) {
            case 'U' -> pacman.image = pacmanUpImage;
            case 'D' -> pacman.image = pacmanDownImage;
            case 'L' -> pacman.image = pacmanLeftImage;
            case 'R' -> pacman.image = pacmanRightImage;
        }
    }

    // ========================= MAIN (for quick run) =========================

    public static void main(String[] args) {
        JFrame frame = new JFrame("Pac-Man");
        PacMan game = new PacMan();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
