package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static be.kuleuven.pylos.game.PylosGameState.MOVE;

public class StudentPlayerBestFit extends PylosPlayer {

    private Map<PylosSphere, Integer> scoreMapSpheresOwn;
    private Map<PylosSphere, Integer> scoreMapSpheresEnemy;
    private Map<PylosLocation, Integer> scoreMapLocations;
    private boolean initialized = false;
    private PylosBoard previousBoard;
    private PylosPlayerColor enemyColor;
    private PylosGameSimulator simulator;

    private final int tresholdToRemove = 600;
    private final int valueHeightInLocationScore = 20;
    private final int reserveScore = 1000;

    private int totalOwnScore = 0;
    private int totalEnemyScore = 0;

    private final int recursionDepth = 2;
    private PylosSphere sphereToMove=null;
    private PylosLocation locationToMove=null;
    private boolean toRemoveFirst = false;
    private boolean toRemoveSec = false;
    private boolean toPass = false;
    private int deltaScore = Integer.MIN_VALUE;

    // check** globale map: key= sphere , value = score // optioneel: 2 scores, verliezend of winnend
    // check** (ben niet zeker of dit nodig is)+(java gebruikt pass by copy bij argumenten) globale Board opslaan, en iedere locatie vergelijkt met de vorige -> nieuwe scores
    // evaluatie fctie: vierkant, eigen ballen in square viscinity, enemy ballen in square viscinity, hoe lager de bal; hoe lager de score
    // eigen vierkant of enemy vierkant? -> hangt ervan af hoeveel ballen in reserve zijn bij ons en de enemy

    //TODO: mischien conditie van winnen veranderen naar welke speler de hoogste totale score heeft?
    //Antwoord Ute: Nee want als al uw bollen op goeie plekken vastliggen, maar ze liggen daar allemaal (je hebt geen reserve meer), ga je en hogere totale score hebben dus dat geeft een vertekend beeld
    //TODO: de vierkanten in de hoeken pikt hij niet op
    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        //Initialize method will have saved the previous board and checked which color this player is + added spheres to map
        if (!initialized) {
            initializeScore(game, board);
        }
        //updateAllScores(board);
        calculateAllScores(board);

        int delta = minMaxRecursie(board,0,this.PLAYER_COLOR);
        System.out.println("Move");
        System.out.println("delta: "+delta);
        game.moveSphere(sphereToMove,locationToMove);
/*
        //step 1
        //Check if we are losing or winning and depending on this info, check for own square first or enemy square first
        boolean winning = false;
        if (board.getReservesSize(this) >= board.getReservesSize(enemyColor)) winning = true;
        boolean moved = false;

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
        // sam: hier houdt de score al rekening mee, in het geval er 2 squares zouden gevuld worden zou de score extreem groot zijn

        if(!moved) {
            PylosLocation locationToMoveTo = null;
            int maxScore = 0;
            for(PylosLocation location: board.getLocations()) {
                if(location.isUsable() && scoreMapLocations.get(location)>maxScore) {
                    //System.out.println("changed: " + scoreMapLocations.get(location) +" "+ scoreMapLocations.get(locationToMoveTo));
                    maxScore = scoreMapLocations.get(location);
                    locationToMoveTo = location;
                }
            }
            PylosSphere sphereToMove = board.getReserve(this);
            int minScore = reserveScore;
            for(PylosSphere sphere: board.getSpheres(this)) {
                if(sphere.canMoveTo(locationToMoveTo) && scoreMapSpheresOwn.get(sphere)<=minScore) {
                    sphereToMove = sphere;
                    minScore = scoreMapSpheresOwn.get(sphereToMove);
                }
            }
            game.moveSphere(sphereToMove, locationToMoveTo);
        }

        previousBoard = board;*/
    }

    //TODO:remove werkt nog niet
    private int minMaxRecursie(PylosBoard board, int depth,PylosPlayerColor color){
        PylosGameState prevState=simulator.getState();
        PylosPlayerColor nextColor;
        int tempDeltaScoreAllSpheres;
        if(color==this.PLAYER_COLOR){
            nextColor=enemyColor;
            tempDeltaScoreAllSpheres = Integer.MIN_VALUE;
        }
        else {
            nextColor=this.PLAYER_COLOR;
            tempDeltaScoreAllSpheres = Integer.MAX_VALUE;
        }

        PylosSphere tempSphere=null;
        PylosLocation tempLocation=null;

        if(prevState == PylosGameState.MOVE) {
            List<PylosLocation> usableLocations = new ArrayList<>();
            List<PylosSphere> usableSpheres = new ArrayList<>();

            //search for every usable location and sphere
            for(PylosLocation location: board.getLocations()){
                if(location.isUsable()){
                    for(PylosSphere sphere: board.getSpheres(color)) {
                        if(!usableSpheres.contains(sphere) && sphere.canMoveTo(location)) {
                            usableSpheres.add(sphere);
                            if(!usableLocations.contains(location)) usableLocations.add(location);
                        }
                    }
                }
            }

            //go over each sphere to each possible location
            for (PylosSphere sphere : usableSpheres) {
                for (PylosLocation location : usableLocations) {
                        if (sphere.canMoveTo(location)) {
                            simulator.moveSphere(sphere, location);

                            calculateAllScores(board);

                            //at the end of the recursion or not? not: keep going
                            int tempDeltaScore;
                            if (depth < recursionDepth) {
                                //TODO: verandert sphere.locatie naar null; ergens een removeSphere te veel?
                                depth++;
                                tempDeltaScore = minMaxRecursie(board,depth , nextColor);
                                System.out.println("depth:" + depth);
                                if(sphere.getLocation() == null){
                                    System.out.println("sefds");
                                }
                            } else {
                                tempDeltaScore = totalOwnScore - totalEnemyScore;
                            }

                            //if new delta is bigger then use that one (own turn)
                            if (color == this.PLAYER_COLOR && tempDeltaScoreAllSpheres <= tempDeltaScore) {
                                double rand = Math.random();
                                if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {
                                } else {
                                    tempDeltaScoreAllSpheres = tempDeltaScore;
                                    tempSphere = sphere;
                                    tempLocation = location;
                                }
                                //if new delta is smaller then use that one (enemy turn)
                            } else if (color == enemyColor && tempDeltaScoreAllSpheres >= tempDeltaScore) {
                                double rand = Math.random();
                                if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {
                                } else {
                                    tempDeltaScoreAllSpheres = tempDeltaScore;
                                    tempSphere = sphere;
                                    tempLocation = location;
                                }
                            }
                            if(sphere.getLocation() == null){
                                System.out.println("sefds");
                            }

                            simulator.undoAddSphere(sphere, prevState, color);
                        }
                    }
                }
            sphereToMove = tempSphere;
            locationToMove = tempLocation;
            return tempDeltaScoreAllSpheres;
        }else if (prevState == PylosGameState.REMOVE_FIRST){

            for (PylosSphere sphere : board.getSpheres(nextColor)) {
                if (sphere.canRemove()) {
                    PylosLocation prevLoc = sphere.getLocation();
                    simulator.removeSphere(sphere);
                    calculateAllScores(board);
                    int tempDeltaScore = minMaxRecursie(board, depth, nextColor);
                    //if new delta is bigger then use that one (own turn)
                    if (color == this.PLAYER_COLOR && tempDeltaScoreAllSpheres <= tempDeltaScore) {
                        double rand = Math.random();
                        if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {
                        } else {
                            tempDeltaScoreAllSpheres = tempDeltaScore;
                            tempSphere = sphere;
                        }
                        //if new delta is smaller then use that one (enemy turn)
                    } else if (color == enemyColor && tempDeltaScoreAllSpheres >= tempDeltaScore) {
                        double rand = Math.random();
                        if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {
                        } else {
                            tempDeltaScoreAllSpheres = tempDeltaScore;
                            tempSphere = sphere;
                        }
                    }
                    simulator.undoRemoveFirstSphere(sphere,prevLoc,prevState,color);
                }
            }
            if(depth==0){
                toRemoveFirst = true;
                sphereToMove=tempSphere;
            }
            return tempDeltaScoreAllSpheres;

        }else if(prevState == PylosGameState.REMOVE_SECOND){
            calculateAllScores(board);
            for (PylosSphere sphere : board.getSpheres(color)) {
                if (sphere.canRemove()) {
                    PylosLocation prevLoc = sphere.getLocation();
                    simulator.removeSphere(sphere);
                    calculateAllScores(board);

                    int tempDeltaScore = minMaxRecursie( board, depth, nextColor);

                    //if new delta is bigger then use that one (own turn)
                    if (color == this.PLAYER_COLOR && tempDeltaScoreAllSpheres <= tempDeltaScore) {
                        double rand = Math.random();
                        if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {
                        } else {
                            tempDeltaScoreAllSpheres = tempDeltaScore;
                            tempSphere = sphere;
                        }
                        //if new delta is smaller then use that one (enemy turn)
                    } else if (color == enemyColor && tempDeltaScoreAllSpheres >= tempDeltaScore) {
                        double rand = Math.random();
                        if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {
                        } else {
                            tempDeltaScoreAllSpheres = tempDeltaScore;
                            tempSphere = sphere;
                        }
                    }
                    simulator.undoRemoveSecondSphere(sphere,prevLoc,prevState,color);
                }
            }

            //houdt rekening mee dat de pass als eerste zet kan gebeuren
            simulator.pass();
            calculateAllScores(board);
            int tempDeltaScore = minMaxRecursie( board, depth, nextColor);

            //if new delta is bigger then use that one (own turn)
            if (color == this.PLAYER_COLOR && tempDeltaScoreAllSpheres <= tempDeltaScore) {
                double rand = Math.random();
                if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {
                } else {
                    tempDeltaScoreAllSpheres = tempDeltaScore;
                }
                //if new delta is smaller then use that one (enemy turn)
            } else if (color == enemyColor && tempDeltaScoreAllSpheres >= tempDeltaScore) {
                double rand = Math.random();
                if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {
                } else {
                    tempDeltaScoreAllSpheres = tempDeltaScore;
                }
            }
            else {
                toRemoveSec=true;
                sphereToMove=tempSphere;
            }

            simulator.undoPass(prevState,color);

            return tempDeltaScoreAllSpheres;

        }else if(prevState== PylosGameState.COMPLETED){
            if(simulator.getWinner() == this.PLAYER_COLOR){
                return Integer.MAX_VALUE;
            }else {
                return totalOwnScore - totalEnemyScore;
            }
        }else if(prevState== PylosGameState.DRAW){
            return totalOwnScore - totalEnemyScore;
        }else return Integer.MIN_VALUE;
    }

    //TODO: da werkt hier maar heel soms
    //Sam: in de huidige implementatie is er geen zekerheid dat er een bal uit het vierkant genomen wordt
    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {

        int delta = minMaxRecursie(board,0,this.PLAYER_COLOR);
        if(toRemoveFirst) {
            System.out.println("remove");
            System.out.println("delta: "+delta);
            toRemoveFirst = false;
            game.removeSphere(sphereToMove);
        }

        /*
        calculateAllScores(board);
        //updateAllScores(board);
        int minimalScore = Integer.MAX_VALUE;
        PylosSphere sphereToMove = null;

        for(PylosSphere sphere: board.getSpheres(this)) {
            if(scoreMapSpheresOwn.get(sphere)<=minimalScore && !sphere.isReserve() && sphere.canRemove()) {
                minimalScore = scoreMapSpheresOwn.get(sphere);
                sphereToMove = sphere;
            }
        }

        //Remove the sphere IF it is on the board
        if(sphereToMove.getLocation()!=null)game.removeSphere(sphereToMove);
        previousBoard = board;
*/
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {

        int delta = minMaxRecursie(board,0,this.PLAYER_COLOR);
        if(toRemoveSec) {
            System.out.println("removeOrPass");
            System.out.println("delta: "+delta);
            toRemoveSec = false;
            game.removeSphere(sphereToMove);
        }else if(toPass){
            toPass = false;
            game.pass();
        }


        /*
        //Gebruik OF calculateAllScores OF updateAllScores want anders ga je bij updaten alles een tweede keer aanpassen.
        calculateAllScores(board);
        //updateAllScores(board);
        boolean remove = false;
        boolean possible = false;
        for(PylosSphere sphere: board.getSpheres(this)) {
            if(scoreMapSpheresOwn.get(sphere)<=tresholdToRemove) {
                remove = true;
            }
            if(sphere.canRemove()) possible = true;
        }
        if(remove && possible) {
            doRemove(game, board);
        }
        else game.pass();
*/
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
                if (scoreMapSpheresOwn.get(tempSphere) < scoreMapSpheresOwn.get(movingSphere)) {
                    movingSphere = tempSphere;
                    movingLocation = tempLocation;
                }
            }
        }

        //step 2 : if we can't make the square with a sphere from the board then we use the reserve (basic) but the location is not yet identified
        if(movingLocation==null) {
            //TODO: voorkeur geven aan welke square, afhankelijk van het niveau waarop, nu neem ik gwn de eerste uit de lijst
            for(PylosLocation location: doableSquares.get(0).getLocations()) {
                if(location.isUsable()) movingLocation = location;
            }
        }


        //step 3 : we fill the square that we just chose
        if (movingLocation != null && movingSphere.canMoveTo(movingLocation)) {
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
                    if (scoreMapSpheresOwn.get(sphere) < scoreMapSpheresOwn.get(movingSphere)) {
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
            if (square.getInSquare(this) == 3 && square.getInSquare(enemyColor) == 0) {
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
    private void initializeScore(PylosGameIF game, PylosBoard board) {

        //Checking what the color of the enemy is
        if (this.PLAYER_COLOR == PylosPlayerColor.LIGHT) {
            enemyColor = PylosPlayerColor.DARK;
        } else enemyColor = PylosPlayerColor.LIGHT;


        scoreMapSpheresOwn = new HashMap<>();
        scoreMapSpheresEnemy = new HashMap<>();
        scoreMapLocations = new HashMap<>();
        simulator = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR,board);

        PylosSphere[] mySpheres = board.getSpheres(this);
        PylosSphere[] enemySpheres = board.getSpheres(enemyColor);

        for (PylosSphere sphere : mySpheres) {
            scoreMapSpheresOwn.put(sphere, reserveScore);
        }

        for (PylosSphere sphere : enemySpheres) {
            scoreMapSpheresEnemy.put(sphere, reserveScore);
        }

        for(PylosLocation location: board.getLocations()) {
            scoreMapLocations.put(location, location.Z*valueHeightInLocationScore);
        }

        previousBoard = board;
        initialized = true;
    }

    //calculate the new scores for all our spheres on the board
    private void calculateAllScores(PylosBoard board) {
        PylosSphere[] mySpheres = board.getSpheres(this);
        PylosSphere[] enemySpheres = board.getSpheres(enemyColor);

        for (PylosSphere sphere : mySpheres) {
            // If the sphere is still in the reserve, we will not need to calculate a score.
            if (!sphere.isReserve()) {
                evaluationFunctionSphere(sphere, board);
            }else {
                scoreMapSpheresOwn.put(sphere, reserveScore);
            }
        }

        int tempvalue=0;
        for(int value : scoreMapSpheresOwn.values()){
            tempvalue += value;
        }
        totalOwnScore=tempvalue;

        for (PylosSphere sphere : enemySpheres) {
            // If the sphere is still in the reserve, we will not need to calculate a score.
            if (!sphere.isReserve()) {
                evaluationFunctionSphereEnemy(sphere, board);
            } else {
                scoreMapSpheresEnemy.put(sphere, reserveScore);
            }
        }

        tempvalue=0;
        for(int value : scoreMapSpheresEnemy.values()){
            tempvalue += value;
        }
        totalEnemyScore=tempvalue;

        for(PylosLocation location : board.getLocations()){
            evaluationFunctionLocationTest(location,board);
        }
    }

    /*gives a score to a sphere
    higher score = better placement

    reserve = 150        // min score is 65 per square

    own spheres --> n*n*15 + 30

    1 own spheres  =  45
    2 own spheres  = 90
    3 own spheres  =  165
    4 own spheres = 0

    enemy spheres --> n*n*15 + 20

    0 enemy spheres = 20
    1 enemy spheres = 35
    2 enemy spheres = 80
    3 enemy spheres = 155

    height bal = z * 30
     */
    private void evaluationFunctionSphere(PylosSphere sphere, PylosBoard board) {

        /*
                STEP 2: Looking for squares in which the sphere is located
         */
        List<PylosSquare> goodSquares = sphere.getLocation().getSquares();

        // after the squares are found calculate the new score
        int score = 0;

        int enemySpheres;
        int ownSpheres;
        int height = sphere.getLocation().Z;

        //Add the score for each square
        for (PylosSquare square : goodSquares) {
            enemySpheres = square.getInSquare(enemyColor);
            ownSpheres = square.getInSquare(this.PLAYER_COLOR);
            score += (enemySpheres * enemySpheres * 100 + 200);

            //If ownSpheres is 4, there already is a square of own color so it would be best to remove this sphere if possible in order to remake the square
            if (ownSpheres != 4) {
                score += (ownSpheres  * ownSpheres  * 100 + 300);
            } //TODO: mis zelfs score verminderen als vierkant vol zit --> of sphere met de minste score in het vierkant score 0 geven

        }
        score += height * 30;

        scoreMapSpheresOwn.replace(sphere, score);
    }


    private void evaluationFunctionSphereEnemy(PylosSphere sphere, PylosBoard board) {

        /*
                STEP 2: Looking for squares in which the sphere is located
         */
        List<PylosSquare> goodSquares = sphere.getLocation().getSquares();


        // after the squares are found calculate the new score
        int score = 0;

        int enemySpheres;
        int ownSpheres;
        int height = sphere.getLocation().Z;

        //Add the score for each square
        for (PylosSquare square : goodSquares) {
            enemySpheres = square.getInSquare(this.PLAYER_COLOR);
            ownSpheres = square.getInSquare(enemyColor);
            score += (enemySpheres * enemySpheres * 100 + 200);

            //If ownSpheres is 4, there already is a square of own color so it would be best to remove this sphere if possible in order to remake the square
            if (ownSpheres != 4) {
                score += (ownSpheres  * ownSpheres  * 100 + 300);
            } //TODO: mis zelfs score verminderen als vierkant vol zit --> of sphere met de minste score in het vierkant score 0 geven

        }
        score += height * 30;

        scoreMapSpheresEnemy.replace(sphere, score);
    }

    private void evaluationFunctionLocationTest(PylosLocation givenLocation, PylosBoard board) {

        // after the squares are found calculate the new score
        int score = 0;

        int enemySpheres;
        int ownSpheres;
        int height = givenLocation.Z;

        //Add the score for each square
        for (PylosSquare square : givenLocation.getSquares()) {
            enemySpheres = square.getInSquare(enemyColor);
            ownSpheres = square.getInSquare(this.PLAYER_COLOR);
            score += (enemySpheres * enemySpheres * 15 + 20);
            score += (ownSpheres  * ownSpheres  * 15 + 30);
        }
        score += height * 50;

        scoreMapLocations.replace(givenLocation, score);

    }


    private void evaluationFunctionLocation(PylosLocation givenLocation, PylosBoard board) {
        int score = 0;
        int ourAlmostSquares=0;
        int enemyAlmostSquares=0;
        for(PylosSquare square: givenLocation.getSquares()) {
            //OPM: bollen die horizontaal/verticaal naast onze locatie liggen, worden dubbel geteld in vergelijking met bollen die diagonaal liggen
            //Dit is goed omdat we sws een horizontale/verticale buur nodig hebben om een bol te leggene daar waar je plots 2 vierkanten kan maken in de volgende beurt
            int ourSpheresInSquare = square.getInSquare(this);
            int enemySpheresInSquare = square.getInSquare(enemyColor);
            score+= ourSpheresInSquare+enemySpheresInSquare;
            if(ourSpheresInSquare==2 && enemySpheresInSquare==0) ourAlmostSquares++;
            if(enemySpheresInSquare==2 && ourSpheresInSquare==0) enemyAlmostSquares++;
        }
        //bonus points: als we met één zet ervoor kunnen zorgen dat je op 2 plekken een vierkant gaat kunnen leggen
        score+=ourAlmostSquares*6;
        score+=enemyAlmostSquares*4;

        //Om ervoor te zorgen dat je in het begin start in het midden, hoe meer squares hoe beter
        score+=givenLocation.getSquares().size();

        scoreMapLocations.replace(givenLocation, score);
    }

    private void updateAllScores(PylosBoard board) {
        PylosLocation[] locationsNow = board.getLocations();
        PylosLocation[] locationsPrev = previousBoard.getLocations();

        for(int i=0; i<locationsNow.length; i++) {
            PylosSphere currentSphere = locationsNow[i].getSphere();
            PylosSphere oldSphere = locationsPrev[i].getSphere();
            if (currentSphere!=oldSphere) {
                //Option 1 the sphere was removed
                if(currentSphere == null) {
                    scoreMapSpheresOwn.replace(oldSphere, reserveScore);
                    for(PylosSquare neighbourSquare: oldSphere.getLocation().getSquares()) {
                        for(PylosLocation neighbourLocation: neighbourSquare.getLocations()) {
                            if(neighbourLocation.isUsed()) {
                                PylosSphere neighbourSphere = neighbourLocation.getSphere();
                                evaluationFunctionSphere(neighbourSphere, board);
                            }
                            else{
                                evaluationFunctionLocation(neighbourLocation, board);
                            }
                        }
                    }
                }

                //Option 2 the sphere was placed
                if(oldSphere == null) {
                    for(PylosSquare neighbourSquare: currentSphere.getLocation().getSquares()) {
                        for(PylosLocation neighbourLocation: neighbourSquare.getLocations()) {
                            if(neighbourLocation.isUsed()) {
                                PylosSphere neighbourSphere = neighbourLocation.getSphere();
                                evaluationFunctionSphere(neighbourSphere, board);
                            }
                            else {
                                evaluationFunctionLocation(neighbourLocation, board);
                            }
                        }
                    }
                }
            }
        }
    }
}
