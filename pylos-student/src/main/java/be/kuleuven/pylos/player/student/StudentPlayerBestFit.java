package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentPlayerBestFit extends PylosPlayer {

    private Map<PylosSphere, Integer> scoreMap;
    private boolean initialized = false;
    private PylosBoard previousBoard;
    private PylosPlayerColor enemyColor;

    private final int reserveScore = 150;

    // check** globale map: key= sphere , value = score // optioneel: 2 scores, verliezend of winnend
    // check** (ben niet zeker of dit nodig is)+(java gebruikt pass by copy bij argumenten) globale Board opslaan, en iedere locatie vergelijkt met de vorige -> nieuwe scores
    // evaluatie fctie: vierkant, eigen ballen in square viscinity, enemy ballen in square viscinity, hoe lager de bal; hoe lager de score
    // eigen vierkant of enemy vierkant? -> hangt ervan af hoeveel ballen in reserve zijn bij ons en de enemy

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        //Initialize method will have saved the previous board and checked which color this player is + added spheres to map
        if (!initialized) initializeScore(board);
        //TODO we moeten het previousboard gebruiken zodat we een updateScores kunnen doen en de scores niet iedere keer allemaal moeten herberekenen.
        //TODO antwoord: ik denk eigenlijk niet dat dit sneller zou zijn dan alle scores op het veld te berekenen, dit zouden we dan kunnen testen
        calculateAllScores(board);

        //step 1
        //Check if we are losing or winning and depending on this info, check for own square first or enemy square first
        boolean winning = false;
        if (board.getReservesSize(this) >= board.getReservesSize(enemyColor)) winning = true;
        boolean moved = false;

        //3 onderdelen : 1- kijken of we aan het winnen zijn 2- zo ja is er een bol die al op het veld ligt en naar boven kan 3- anders een uit reserve

        //STEP 1: check for squares (yours or your enemies first depending on winning)
        if (winning) {
            moved = tryToMakeOwnSquare(game, board);
            if (!moved) moved = tryToSabotageEnemySquare(game, board);
        } else {
            moved = tryToSabotageEnemySquare(game, board);
            if (!moved) moved = tryToMakeOwnSquare(game, board);
        }

        //STEP 2: put the sphere somewhere near most other spheres
        //Mogelijke verbeteringen hier: kijken als je bvb drie bollen dicht bij elkaar hebt en met 1 te zetten, 2 mogelijke square makings creeert
        //Idem bij enemy en zo een plek proberen saboteren

        //TODO: ik zou hier de beste plek proberen zoeken ook door een scoresysteem? Dus hier eens berekenen enkel voor lege, legbare plekken. Hogere score hoe meer bollen van eenzelfde kleur er naast liggen en we willen een bol leggen op de hoogste score.
        //TODO: ik dacht hiervoor die simulatie te kunnen gebruiken en dan de bal te plaatsen waar hij de hoogste score zou krijgen. Maar weet nog niet goed hoe wat met die simulatie mogelijk is

    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        calculateAllScores(board);

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {

    }

    private boolean tryToMakeOwnSquare(PylosGameIF game, PylosBoard board) {
        boolean moved = false;

        //check 1 : can we make our own square?
        List<PylosSquare> squaresToComplete = getPossibleCompleteOwnSquares(board);
        if (!squaresToComplete.isEmpty()) {
            moved = makeOrSabotageBestSquareFromList(game, board, squaresToComplete);
        }

        return moved;
    }

    private boolean tryToSabotageEnemySquare(PylosGameIF game, PylosBoard board) {
        boolean moved = false;

        List<PylosSquare> squaresToSabotage = getPossibleCompleteEnemySquares(board);
        if (!squaresToSabotage.isEmpty()) {
            moved = makeOrSabotageBestSquareFromList(game, board, squaresToSabotage);
        }
        return moved;
    }

    private boolean makeOrSabotageBestSquareFromList(PylosGameIF game, PylosBoard board, List<PylosSquare> doableSquares) {
        //Initialize moving values, should be overwrited anyway
        //TODO: wat doen we als de reservelijst op is? Er is een kans dat er gewoon geen zet mogelijk is?
        //TODO: antwoord: als de reservelijst op is dan hebben we of gewonnen of verloren. Game is zowiezo dan gedaan in onze zet of die van de tegenstander hierna
        PylosSphere movingSphere = board.getReserve(this);
        PylosLocation movingLocation = null;

        // step 1 : can we make a square by moving a ball on the board?
        PylosSphere tempSphere = movingSphere;
        PylosLocation tempLocation = movingLocation;

        for (PylosSquare square : doableSquares) {
            for (PylosLocation location : square.getLocations()) {
                tempSphere = getMovableSquareFromBoardTo(board, location);
                if (tempSphere != null) tempLocation = location;
            }
            //check: did we get even one square where we can make it with a sphere from the board?
            if (tempSphere != null) {
                //If this best tempSphere from the board has a lower score than movingsphere, we will use it
                if (scoreMap.get(tempSphere) < scoreMap.get(movingSphere)) {
                    movingSphere = tempSphere;
                    movingLocation = tempLocation;
                }
            }
        }

        //step 2 : we fill the square that we just chose
        if (movingSphere.canMoveTo(movingLocation)) {
            game.moveSphere(movingSphere, movingLocation);
            return true;
        }

        return false;
    }

    private PylosSphere getMovableSquareFromBoardTo(PylosBoard board, PylosLocation location) {
        PylosSphere movingSphere = null;
        for (PylosSphere sphere : board.getSpheres(this)) {
            if (!sphere.isReserve() && sphere.canMoveTo(location)) {
                if (movingSphere != null) {
                    if (scoreMap.get(sphere) < scoreMap.get(movingSphere)) {
                        movingSphere = sphere;
                    }
                } else {
                    movingSphere = sphere;
                }

            }
        }
        return movingSphere;
    }


    //check for the squares that are almost complete (own color)
    private List<PylosSquare> getPossibleCompleteOwnSquares(PylosBoard board) {
        List<PylosSquare> squares = new ArrayList<>();

        for (PylosSquare square : board.getAllSquares()) {
            if (square.getInSquare(this) == 3) {
                squares.add(square);
            }
        }

        return squares;
    }

    //check for the squares that are almost complete (enemy color)
    private List<PylosSquare> getPossibleCompleteEnemySquares(PylosBoard board) {
        List<PylosSquare> squares = new ArrayList<>();

        for (PylosSquare square : board.getAllSquares()) {
            if (square.getInSquare(enemyColor) == 3) {
                squares.add(square);
            }
        }

        return squares;
    }

    // initializing the map that contains the score per sphere and save the first board
    private void initializeScore(PylosBoard board) {

        scoreMap = new HashMap<>();
        PylosSphere[] mySpheres = board.getSpheres(this);

        for (PylosSphere sphere : mySpheres) {
            scoreMap.put(sphere, reserveScore);
        }

        //Checking what the color of the enemy is
        if (this.PLAYER_COLOR == PylosPlayerColor.LIGHT) {
            enemyColor = PylosPlayerColor.DARK;
        } else enemyColor = PylosPlayerColor.LIGHT;

        previousBoard = board;
        initialized = true;
    }

    //calculate the new scores for all our spheres on the board
    private void calculateAllScores(PylosBoard board) {
        PylosSphere[] mySpheres = board.getSpheres(this);

        for (PylosSphere sphere : mySpheres) {

            // If the sphere is still in the reserve, we will not need to calculate a score.
            if (!sphere.isReserve()) {
                evaluationFunction(sphere, board);

            }

        }
    }

    /*gives a score to a sphere
    higher score = better placement

    reserve = 150        // min score is 50 per square

    own spheres --> (n-1)*(n-1)*15 + 30 ... gebruik mss toch geen algemene formule wegens de reden gegeven bij 3 spheres
            Vergeet niet: uw eigen bol wordt in uw redenering ook meegeteld als ownSpheres maw opties zijn 1,2,3,4

    1 own spheres  = 30  --> 45
    2 own spheres  = 45 --> 90
    3 own spheres  = 90 --> 165
    4 own spheres = 0

    enemy spheres --> n*n*15 + 20

    0 enemy spheres = 20
    1 enemy spheres = 35
    2 enemy spheres = 80
    3 enemy spheres = 155

    height bal = z * 30
     */
    private void evaluationFunction(PylosSphere sphere, PylosBoard board) {

        /*
                STEP 2: Looking for squares in which the sphere is located
         */
        List<PylosSquare> goodSquares = new ArrayList<>();

        //searches the squares where the sphere is in; min 1 , max 4
        //TODO: mogelijke verbetering qua snelheid: je checkt van elke square maar 1 locatie en pas als die max 1 verschilt qua coordinaten met eigen locatie, kijk je naar de rest. Gaat wel enkel beter zijn bij grotere spelborden
        //TODO: het board blijft altijd dezelfde grootte normaal
        for (PylosSquare square : board.getAllSquares()) {
            for (PylosLocation location : square.getLocations()) {
                if (sphere.getLocation() == location) {
                    goodSquares.add(square);
                    break;
                }
            }
            if (goodSquares.size() == 4) break;
        }

        // after the squares are found calculate the new score
        int score = 0;

        int enemySpheres;
        int ownSpheres;
        int height = sphere.getLocation().Z;

        //Add the score for each square
        for (PylosSquare square : goodSquares) {
            enemySpheres = square.getInSquare(enemyColor);
            ownSpheres = square.getInSquare(this.PLAYER_COLOR);
            score += (enemySpheres * enemySpheres * 15 + 20);
            //If ownSpheres is 4, there already is a square of own color so it would be best to remove this sphere if possible in order to remake the square
            if (ownSpheres != 4) {
                score += (ownSpheres  * ownSpheres  * 15 + 30);
            } //TODO: mis zelfs score verminderen als vierkant vol zit --> of sphere met de minste score in het vierkant score 0 geven

        }
        score += height * 30;

        scoreMap.replace(sphere, score);

    }
}
