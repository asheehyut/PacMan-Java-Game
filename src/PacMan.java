import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.HashSet;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer; // <-- THIS is the one we want

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

        double speedMultipyer = 0.8;

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
                case 'U' -> {
                    this.velocityX = 0;
                    this.velocityY = -speed;
                }
                case 'D' -> {
                    this.velocityX = 0;
                    this.velocityY = speed;
                }
                case 'L' -> {
                    this.velocityX = -speed;
                    this.velocityY = 0;
                }
                case 'R' -> {
                    this.velocityX = speed;
                    this.velocityY = 0;
                }
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
            this.velocityX = 0;
            this.velocityY = 0;
        }
    }


    // ===== GRAPH NODES =====
    class Node {
        int r, c; // row, col in tileMap
        java.util.List<Node> neighbors = new ArrayList<>();

        Node(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;
    private boolean scared = false;
    private long scaredTimeEnd = 0;
    private final int SCARED_DURATION = 7000;

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;
    private Image scaredGhostImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

    private Image powerFoodImage;

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

    // GRAPH: one Node per walkable tile
    Map<String, Node> graph = new HashMap<>();

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
        buildGraph(); // build graph from tile map
        initializeGhosts();
        startGameLoop();
    }

    private void loadImages() {
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();
        scaredGhostImage = new ImageIcon(getClass().getResource("./scaredGhost.png")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();

        powerFoodImage = new ImageIcon(getClass().getResource("./powerFood.png")).getImage();
    }

    private void initializeGhosts() {
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
    }

    private void startGameLoop() {
        gameLoop = new Timer(50, this); // 20 FPS
        gameLoop.start();
    }

    public void loadMap() {
        walls = new HashSet<>();
        foods = new HashSet<>();
        ghosts = new HashSet<>();

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char tile = row.charAt(c);

                int x = c * tileSize;
                int y = r * tileSize;

                if (tile == 'X') { // wall
                    Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                    walls.add(wall);
                } else if (tile == 'b') { // Inky (blue)
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = 'b';
                    ghosts.add(ghost);
                } else if (tile == 'o') { // Clyde (orange)
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = 'o';
                    ghosts.add(ghost);
                } else if (tile == 'p') { // Pinky
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = 'p';
                    ghosts.add(ghost);
                } else if (tile == 'r') { // Blinky (red)
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghost.ghostType = 'r';
                    ghosts.add(ghost);
                } else if (tile == 'P') { // pacman
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                } else if (tile == ' ') {
                    Block food = new Block(null, x + 14, y + 14, 4, 4);
                    foods.add(food);
                }

            }
        }
        // Hardcoded power food locations (four corners)
        Block powerFood1 = new Block(powerFoodImage, 8 + tileSize, 8 + tileSize, 16, 16); // top-left
        Block powerFood2 = new Block(powerFoodImage, (columnCount - 1) * tileSize + 8 - tileSize, 8 + tileSize, 16, 16); // top-right
        Block powerFood3 = new Block(powerFoodImage, 8 + tileSize, (rowCount - 1) * tileSize + 8 - tileSize, 16, 16); // bottom-left
        Block powerFood4 = new Block(powerFoodImage, (columnCount - 1) * tileSize + 8 - tileSize, (rowCount - 1) * tileSize + 8 - tileSize, 16, 16); // bottom-right

        foods.add(powerFood1);
        foods.add(powerFood2);
        foods.add(powerFood3);
        foods.add(powerFood4);
    }

    // ===== GRAPH HELPERS =====

    private String key(int r, int c) {
        return r + "," + c;
    }

    // Build graph: node for non-wall tiles, edges to 4-neighbors
    private void buildGraph() {
        graph.clear();

        // Create nodes
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                char tile = tileMap[r].charAt(c);
                if (tile != 'X') { // walkable
                    Node node = new Node(r, c);
                    graph.put(key(r, c), node);
                }
            }
        }

        // Connect neighbors
        int[][] dirsRC = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (Node node : graph.values()) {
            for (int[] d : dirsRC) {
                int nr = node.r + d[0];
                int nc = node.c + d[1];
                Node nb = graph.get(key(nr, nc));
                if (nb != null) {
                    node.neighbors.add(nb);
                }
            }
        }
    }

    private boolean isCenteredOnTile(Block b) {
        return (b.x % tileSize == 0) && (b.y % tileSize == 0);
    }

    private Node getNodeForBlock(Block b) {
        int c = b.x / tileSize;
        int r = b.y / tileSize;
        return graph.get(key(r, c));
    }

    // A tile a few steps ahead of Pac-Man, used for Pinky/Inky targeting
    private Node getAheadOfPacmanNode(int tilesAhead) {
        int r = pacman.y / tileSize;
        int c = pacman.x / tileSize;

        switch (pacman.direction) {
            case 'U' -> r -= tilesAhead;
            case 'D' -> r += tilesAhead;
            case 'L' -> c -= tilesAhead;
            case 'R' -> c += tilesAhead;
        }

        // clamp within bounds
        r = Math.max(0, Math.min(rowCount - 1, r));
        c = Math.max(0, Math.min(columnCount - 1, c));

        Node target = graph.get(key(r, c));
        if (target == null) {
            // if it's a wall or invalid, fall back to Pac-Man's tile
            return getNodeForBlock(pacman);
        }
        return target;
    }

    // ===== BFS (for Blinky, part of Inky) =====

    // returns next Node on shortest path from start to goal
    private Node bfsNextStep(Node start, Node goal) {
        if (start == null || goal == null)
            return null;
        if (start == goal)
            return start;

        Queue<Node> queue = new ArrayDeque<>();
        Map<Node, Node> parent = new HashMap<>();
        Set<Node> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current == goal)
                break;

            for (Node nb : current.neighbors) {
                if (!visited.contains(nb)) {
                    visited.add(nb);
                    parent.put(nb, current);
                    queue.add(nb);
                }
            }
        }

        if (!parent.containsKey(goal)) {
            return null; // no path
        }

        // reconstruct to get first step after start
        Node step = goal;
        Node prev = parent.get(step);
        while (prev != null && prev != start) {
            step = prev;
            prev = parent.get(step);
        }

        return step;
    }

    // ===== A* (for Pinky, part of Inky) =====

    private int heuristic(Node a, Node b) {
        // Manhattan distance heuristic
        return Math.abs(a.r - b.r) + Math.abs(a.c - b.c);
    }

    private Node aStarNextStep(Node start, Node goal) {
        if (start == null || goal == null)
            return null;
        if (start == goal)
            return start;

        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingInt(n -> gScore.getOrDefault(n, Integer.MAX_VALUE) + heuristic(n, goal)));
        Set<Node> closed = new HashSet<>();
        parent.clear();
        gScore.clear();

        gScore.put(start, 0);
        open.add(start);

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current == goal)
                break;

            if (!closed.add(current))
                continue;

            int currentG = gScore.get(current);

            for (Node nb : current.neighbors) {
                if (closed.contains(nb))
                    continue;

                int tentativeG = currentG + 1; // cost per edge = 1
                int oldG = gScore.getOrDefault(nb, Integer.MAX_VALUE);

                if (tentativeG < oldG) {
                    gScore.put(nb, tentativeG);
                    parent.put(nb, current);
                    // re-add to open with better score
                    open.remove(nb);
                    open.add(nb);
                }
            }
        }

        if (!parent.containsKey(goal)) {
            return null;
        }

        Node step = goal;
        Node prev = parent.get(step);
        while (prev != null && prev != start) {
            step = prev;
            prev = parent.get(step);
        }
        return step;
    }

    //==========Hybrid========
    private Node hybridNextStep(Node ghostNode) {
        // pinky
        Node pinkyTarget = getAheadOfPacmanNode(4); // 4 tiles ahead
        Node aStep = aStarNextStep(ghostNode, pinkyTarget);

        // blinky
        Node blinkyTarget = getNodeForBlock(pacman);
        Node bStep = bfsNextStep(ghostNode, blinkyTarget);

        // combo
        // if A* gives valid step, use it 70% of the time
        if (aStep != null && Math.random() < 0.7) {
            return aStep;
        }
        // Otherwise BFS
        return bStep;
    }
//===============================================

    //scared nextstep
    private Node scaredNextStep(Block ghost) {
        if (!isCenteredOnTile(ghost)) return null;

        Node ghostNode = getNodeForBlock(ghost);
        Node pacNode = getNodeForBlock(pacman);
        if (ghostNode == null || pacNode == null) return null;

        // Pick the neighbor farthest from Pac-Man
        Node bestNode = null;
        int maxDist = -1;

        for (Node nb : ghostNode.neighbors) {
            int dist = Math.abs(nb.r - pacNode.r) + Math.abs(nb.c - pacNode.c);
            if (dist > maxDist) {
                maxDist = dist;
                bestNode = nb;
            }
        }

        return bestNode;
    }

    // reused maps for A*
    private Map<Node, Node> parent = new HashMap<>();
    private Map<Node, Integer> gScore = new HashMap<>();

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
        checkPowerFoodCollision();

        if (foods.isEmpty()) {
            loadMap();
            buildGraph();
            resetPositions();
        }
        updateScaredMode();
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
                handleGhostCollision(ghost);
                return;
            }

            else if (!scared){
                switch (ghost.ghostType) {
                    case 'r' -> moveRedGhost(ghost); // Blinky: BFS
                    case 'p' -> movePinkGhost(ghost); // Pinky: A*
                    case 'b' -> moveBlueGhost(ghost); // Inky: hybrid BFS + A*
                    case 'o' -> moveOrangeGhost(ghost); // Clyde: random
                    default -> moveGenericGhost(ghost);
                }
            }

            else { moveScaredGhost(ghost); }

        }
    }


    // Individual Ghosts

    private void moveScaredGhost(Block ghost) {
        if (!isCenteredOnTile(ghost)) {
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            checkWallCollision(ghost);
            return;
        }

        Node next = scaredNextStep(ghost);
        scaredRun(ghost, next);
    }

    // Blinky – BFS shortest path directly to Pac-Man
    private void moveRedGhost(Block ghost) {
        bfsChase(ghost, getNodeForBlock(pacman));
    }

    // Pinky – A* path to a tile ahead of Pac-Man
    private void movePinkGhost(Block ghost) {
        Node targetAhead = getAheadOfPacmanNode(3);
        aStarChase(ghost, targetAhead);
    }

    private void moveBlueGhost(Block ghost) {
        hybridChase(ghost);
    }


    // Clyde – random-ish wandering (default / random behavior)
    private void moveOrangeGhost(Block ghost) {
        // occasionally pick a random new direction when centered
        if (isCenteredOnTile(ghost) && random.nextInt(10) == 0) {
            char newDir = directions[random.nextInt(directions.length)];
            ghost.updateDirection(newDir);
        }
        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);
    }

    // helper: BFS chase
    private void bfsChase(Block ghost, Node target) {
        if (!isCenteredOnTile(ghost)) {
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            checkWallCollision(ghost);
            return;
        }

        Node ghostNode = getNodeForBlock(ghost);
        if (ghostNode == null || target == null) {
            moveGenericGhost(ghost);
            return;
        }

        Node next = bfsNextStep(ghostNode, target);
        if (next != null && next != ghostNode) {
            int dr = next.r - ghostNode.r;
            int dc = next.c - ghostNode.c;

            if (dr == -1 && dc == 0)
                ghost.updateDirection('U');
            else if (dr == 1 && dc == 0)
                ghost.updateDirection('D');
            else if (dr == 0 && dc == -1)
                ghost.updateDirection('L');
            else if (dr == 0 && dc == 1)
                ghost.updateDirection('R');
        }

        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);
    }

    // Shared helper: A* chase
    private void aStarChase(Block ghost, Node target) {
        if (!isCenteredOnTile(ghost)) {
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            checkWallCollision(ghost);
            return;
        }

        Node ghostNode = getNodeForBlock(ghost);
        if (ghostNode == null || target == null) {
            moveGenericGhost(ghost);
            return;
        }

        Node next = aStarNextStep(ghostNode, target);
        if (next != null && next != ghostNode) {
            int dr = next.r - ghostNode.r;
            int dc = next.c - ghostNode.c;

            if (dr == -1 && dc == 0)
                ghost.updateDirection('U');
            else if (dr == 1 && dc == 0)
                ghost.updateDirection('D');
            else if (dr == 0 && dc == -1)
                ghost.updateDirection('L');
            else if (dr == 0 && dc == 1)
                ghost.updateDirection('R');
        }

        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);
    }

    //hybrid chase helper
    private void hybridChase(Block ghost) {
        if (!isCenteredOnTile(ghost)) {
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            checkWallCollision(ghost);
            return;
        }

        Node ghostNode = getNodeForBlock(ghost);
        if (ghostNode == null) {
            moveGenericGhost(ghost);
            return;
        }

        Node next = hybridNextStep(ghostNode);
        if (next == null || next == ghostNode) {
            moveGenericGhost(ghost);
            return;
        }

        int dr = next.r - ghostNode.r;
        int dc = next.c - ghostNode.c;

        if (dr == -1 && dc == 0) ghost.updateDirection('U');
        else if (dr == 1 && dc == 0) ghost.updateDirection('D');
        else if (dr == 0 && dc == -1) ghost.updateDirection('L');
        else if (dr == 0 && dc == 1) ghost.updateDirection('R');

        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);
    }

    private void scaredRun(Block ghost, Node next) {
        if (next == null) return;

        Node ghostNode = getNodeForBlock(ghost);
        if (ghostNode == null) return;

        int dr = next.r - ghostNode.r;
        int dc = next.c - ghostNode.c;

        if (dr == -1 && dc == 0) ghost.updateDirection('U');
        else if (dr == 1 && dc == 0) ghost.updateDirection('D');
        else if (dr == 0 && dc == -1) ghost.updateDirection('L');
        else if (dr == 0 && dc == 1) ghost.updateDirection('R');

        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);
    }


    private void moveGenericGhost(Block ghost) {
        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);
    }

    //gpt helped define tunnel logic
    private void checkWallCollision(Block block) {
        // Define tunnel row
        int tunnelRow = 9; // adjust to match your tunnel row

        // Tunnel wrap: teleport to opposite side
        if (block.y / tileSize == tunnelRow) {
            if (block.x + block.width < 0) {          // exited left
                block.x = boardWidth;
            } else if (block.x > boardWidth) {       // exited right
                block.x = -block.width;
            }
        }

        // Normal wall collision for everything else
        for (Block wall : walls) {
            // Skip walls in tunnel row (already handled)
            if (block.y / tileSize == tunnelRow && wall.y / tileSize == tunnelRow) {
                continue;
            }

            if (collision(block, wall) ||
                    block.y < 0 ||
                    block.y + block.height > boardHeight) {

                // revert movement
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


    private void handleGhostCollision(Block ghost) {
        if (!scared){
            lives--;
            if (lives <= 0) {
                gameOver = true;
            } else {
                resetPositions();
            }
        }
        else{
            score += 500;
            returnGhostHome(ghost);

        }

    }

    private void returnGhostHome(Block ghost) {
        ghost.x = ghost.startX;
        ghost.y = ghost.startY;
        ghost.velocityX = 0;
        ghost.velocityY = 0;
        ghost.direction = 'U';      // or any default
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

    private void checkPowerFoodCollision() {
        Block eaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                eaten = food;
                score += 100;
                for (Block ghost : ghosts) {
                    ghost.image = scaredGhostImage;
                }
                scared = true;
                scaredTimeEnd = System.currentTimeMillis() + SCARED_DURATION;

            }
        }
        if (eaten != null) {
            foods.remove(eaten);
        }
    }

    private void updateScaredMode() {
        if (scared && System.currentTimeMillis() >= scaredTimeEnd) {
            scared = false;
            for (Block ghost : ghosts) {
                switch (ghost.ghostType) {
                    case 'r' -> ghost.image = redGhostImage;    // Blinky
                    case 'p' -> ghost.image = pinkGhostImage;   // Pinky
                    case 'b' -> ghost.image = blueGhostImage;   // Inky
                    case 'o' -> ghost.image = orangeGhostImage; // Clyde
                }
            }

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
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!gameOver) {
            handleKeyPress(e); // capture input immediately
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) {
            restartGame(); // keep this if you like space to restart
        }
    }

    private void restartGame() {
        loadMap();
        buildGraph();
        resetPositions();
        lives = 3;
        score = 0;
        gameOver = false;
        gameLoop.start();
    }

    private void handleKeyPress(KeyEvent e) {
        // queue the direction – actual turn happens in movePacman()
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> queuedDirection = 'U';
            case KeyEvent.VK_DOWN -> queuedDirection = 'D';
            case KeyEvent.VK_LEFT -> queuedDirection = 'L';
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

    // Main Program

    public static void main(String[] args) {
        JFrame frame = new JFrame("Pac-Man (Graphs: BFS + A*)");
        PacMan game = new PacMan();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
