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

    private final int valueHeightInLocationScore = 25;
    private final int reserveScore = 25;

    private int totalOwnScore = 0;
    private int totalEnemyScore = 0;

    private final int recursionDepth = 2;
    private final int MaxNumberOfProbableMoves = 15;

    private PylosSphere sphereToMove=null;
    private PylosLocation locationToMove=null;
    private boolean toRemoveSec = false;


    // check** globale map: key= sphere , value = score // optioneel: 2 scores, verliezend of winnend
    // check** (ben niet zeker of dit nodig is)+(java gebruikt pass by copy bij argumenten) globale Board opslaan, en iedere locatie vergelijkt met de vorige -> nieuwe scores
    // evaluatie fctie: vierkant, eigen ballen in square viscinity, enemy ballen in square viscinity, hoe lager de bal; hoe lager de score
    // eigen vierkant of enemy vierkant? -> hangt ervan af hoeveel ballen in reserve zijn bij ons en de enemy


    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        sphereToMove=null;
        locationToMove=null;

        //Initialize method will have saved the previous board and checked which color this player is + added spheres to map
        if (!initialized) {
            initializeScore(game, board);
        }

        if (this.PLAYER_COLOR == PylosPlayerColor.LIGHT) {
            enemyColor = PylosPlayerColor.DARK;
        } else enemyColor = PylosPlayerColor.LIGHT;

        simulator=new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);
        calculateAllScores(board);

        minMaxRecursie(board,0,this.PLAYER_COLOR);
        if(locationToMove!=null && !sphereToMove.canMoveTo(locationToMove)){
            System.out.print("sdf");
        }

        if(locationToMove==null){
            System.out.print("sdf");
        }
        game.moveSphere(sphereToMove,locationToMove);

    }


    private int minMaxRecursie(PylosBoard board, int depth,PylosPlayerColor color){
        PylosGameState currentState=simulator.getState();

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

        if(currentState == PylosGameState.MOVE) {
            PylosSphere tempSphere = null;
            PylosLocation tempLocation = null;
            List<PylosLocation> usableLocations = new ArrayList<>();
            List<PylosSphere> usableSpheres = new ArrayList<>();

            //search for every usable location and sphere
            for(PylosLocation location: board.getLocations()){
                if(location.isUsable()){
                    for(PylosSphere sphere: board.getSpheres(color)) {
                        if(sphere.canMoveTo(location)) {
                            if (!usableSpheres.contains(sphere)) usableSpheres.add(sphere);
                            if (!usableLocations.contains(location)) usableLocations.add(location);
                        }
                    }
                }
            }

            PylosSphere[] probableSpheres = new PylosSphere[MaxNumberOfProbableMoves];
            PylosLocation[] probableLocations = new PylosLocation[MaxNumberOfProbableMoves];
            int[] probableScores = new int[MaxNumberOfProbableMoves];

            for(int i = 0;i<MaxNumberOfProbableMoves;i++){
                if(color==this.PLAYER_COLOR) {
                    probableScores[i] = Integer.MIN_VALUE;
                }else probableScores[i] = Integer.MAX_VALUE;
            }

            int smallestScore = probableScores[0];
            int smallestScoreIndex = 0;

            for (PylosSphere sphere : usableSpheres) {
                for (PylosLocation location : usableLocations) {
                    if(sphere.canMoveTo(location)) {

                        boolean isReserve = sphere.isReserve();

                        PylosLocation prevLoc = sphere.getLocation();
                        prevState = simulator.getState();
                        if(simulator.getColor() != color){
                            System.out.print("sdf");
                        }
                        simulator.moveSphere(sphere, location);
                        calculateAllScores(board);

                        int tempDeltaScore = totalOwnScore - totalEnemyScore;
                        if(color==this.PLAYER_COLOR && tempDeltaScore >= smallestScore ) {

                            probableSpheres[smallestScoreIndex] = sphere;
                            probableLocations[smallestScoreIndex] = location;
                            probableScores[smallestScoreIndex] = tempDeltaScore;
                            smallestScore = tempDeltaScore;

                            for (int i = 0; i < MaxNumberOfProbableMoves; i++) {
                                if (smallestScore >= probableScores[i]) {
                                    smallestScore = probableScores[i];
                                    smallestScoreIndex = i;
                                }
                            }

                        }else if(color==enemyColor&& tempDeltaScore <= smallestScore ){

                            probableSpheres[smallestScoreIndex] = sphere;
                            probableLocations[smallestScoreIndex] = location;
                            probableScores[smallestScoreIndex] = tempDeltaScore;
                            smallestScore = tempDeltaScore;

                            for (int i = 0; i < MaxNumberOfProbableMoves; i++) {
                                if (smallestScore >= probableScores[i]) {
                                    smallestScore = probableScores[i];
                                    smallestScoreIndex = i;
                                }
                            }

                        }

                        if (isReserve) simulator.undoAddSphere(sphere, prevState, color);
                        else simulator.undoMoveSphere(sphere, prevLoc, prevState, color);
                    }
                }
            }

            if(usableSpheres.size() == 1 && usableLocations.size()==1){
                PylosSphere sphere =usableSpheres.get(0);
                PylosLocation location = usableLocations.get(0);

                PylosLocation prevLoc = sphere.getLocation();
                prevState = simulator.getState();

                boolean isReserve = false;
                if (sphere.isReserve()) isReserve = true;

                simulator.moveSphere(sphere, location);

                calculateAllScores(board);
                int tempScore =  totalOwnScore - totalEnemyScore;

                if (isReserve) simulator.undoAddSphere(sphere, prevState, color);
                else simulator.undoMoveSphere(sphere, prevLoc, prevState, color);
                if(depth==0){
                    sphereToMove = sphere;
                    locationToMove = location;
                }
                return tempScore;
            }

            //go over each sphere to each possible location
            /*
            for (PylosSphere sphere : probableSpheres) {
                for (PylosLocation location : probableLocations) {*/
            for(int i=0;i<MaxNumberOfProbableMoves;i++) {
                PylosSphere sphere = probableSpheres[i];
                PylosLocation location = probableLocations[i];
                if (sphere != null && location != null && sphere.canMoveTo(location)) {
                    boolean isReserve = false;
                    if (sphere.isReserve()) isReserve = true;

                    PylosLocation prevLoc = sphere.getLocation();
                    prevState = simulator.getState();

                    simulator.moveSphere(sphere, location);

                    calculateAllScores(board);
                    int tempDeltaScore;

                    //at the end of the recursion or not? not: keep going
                    int tempDepth = depth;
                    if (tempDepth < recursionDepth) {

                        tempDepth++;
                        tempDeltaScore = minMaxRecursie(board, tempDepth, nextColor);
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
                        }
                    }

                    if (isReserve) simulator.undoAddSphere(sphere, prevState, color);
                    else simulator.undoMoveSphere(sphere, prevLoc, prevState, color);
                }
            }

            sphereToMove = tempSphere;
            locationToMove = tempLocation;

            return tempDeltaScoreAllSpheres;

        }else if (currentState == PylosGameState.REMOVE_FIRST){
            PylosSphere tempSphere = null;


            if(nextColor==this.PLAYER_COLOR){
                tempDeltaScoreAllSpheres = Integer.MIN_VALUE;
            }
            else {
                tempDeltaScoreAllSpheres = Integer.MAX_VALUE;
            }

            /**
             * Ik heb dit aangepast zodat de eerste remove ALTIJD eentje is uit het net gemaakte vierkant

             Eerste twee lijnen van de forlus waren zo:

             for (PylosSphere sphere : board.getSpheres(nextColor)) {

             if (!sphere.isReserve() && sphere.canRemove()) {


             */

            PylosSphere[] spheresFromSquares = getSpheresInSquare(board, nextColor);

            for(PylosSphere sphere: spheresFromSquares) {

                if(sphere.canRemove()) {
                    PylosLocation prevLoc = sphere.getLocation();
                    prevState=simulator.getState();
                    simulator.removeSphere(sphere);

                    calculateAllScores(board);
                    int tempDepth = depth;
                    tempDepth++;
                    int tempDeltaScore = minMaxRecursie(board, tempDepth, nextColor);

                    //if new delta is bigger then use that one (own turn)
                    if (nextColor == this.PLAYER_COLOR) {
                        if (tempDeltaScoreAllSpheres <= tempDeltaScore) {
                            double rand = Math.random();
                            if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {}
                            else {
                                tempDeltaScoreAllSpheres = tempDeltaScore;
                                tempSphere = sphere;
                            }
                        }
                        //if new delta is smaller then use that one (enemy turn)
                    } else if (nextColor == enemyColor && tempDeltaScoreAllSpheres >= tempDeltaScore) {
                        double rand = Math.random();
                        if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {}
                        else {
                            tempDeltaScoreAllSpheres = tempDeltaScore;
                            tempSphere = sphere;
                        }
                    }
                    simulator.undoRemoveFirstSphere(sphere,prevLoc,prevState,nextColor);

                }

            }

            sphereToMove=tempSphere;


            return tempDeltaScoreAllSpheres;



        }else if(currentState == PylosGameState.REMOVE_SECOND){
            PylosSphere tempSphere = null;

            if(color==this.PLAYER_COLOR){
                tempDeltaScoreAllSpheres = Integer.MIN_VALUE;
            }
            else {
                tempDeltaScoreAllSpheres = Integer.MAX_VALUE;
            }

            for (PylosSphere sphere : board.getSpheres(color)) {

                if (!sphere.isReserve() && sphere.canRemove()) {
                    PylosLocation prevLoc = sphere.getLocation();
                    prevState=simulator.getState();
                    int tempDepth = depth;
                    simulator.removeSphere(sphere);

                    tempDepth++;

                    calculateAllScores(board);

                    int tempDeltaScore = minMaxRecursie(board, tempDepth, nextColor);

                    //if new delta is bigger then use that one (own turn)
                    if (color == this.PLAYER_COLOR) {
                        if (tempDeltaScoreAllSpheres <= tempDeltaScore) {
                            double rand = Math.random();
                            if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {}
                            else {
                                if(depth==0){
                                    tempSphere = sphere;
                                }
                                tempDeltaScoreAllSpheres = tempDeltaScore;
                            }
                        }
                        //if new delta is smaller then use that one (enemy turn)
                    } else if (color == enemyColor && tempDeltaScoreAllSpheres >= tempDeltaScore) {
                        double rand = Math.random();
                        if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {}
                        else {
                            tempDeltaScoreAllSpheres = tempDeltaScore;
                        }
                    }
                    simulator.undoRemoveSecondSphere(sphere,prevLoc,prevState,color);
                }
            }
            boolean toPass=false;
            //houdt rekening mee dat de pass als eerste zet kan gebeuren
            simulator.pass();
            calculateAllScores(board);
            int tempDepth = depth;
            tempDepth++;
            int tempDeltaScore = minMaxRecursie( board, tempDepth, nextColor);

            //if new delta is bigger then use that one (own turn)
            if (color == this.PLAYER_COLOR && tempDeltaScoreAllSpheres <= tempDeltaScore) {
                double rand = Math.random();
                if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {
                } else {
                    tempDeltaScoreAllSpheres = tempDeltaScore;
                    toPass=true;
                }
                //if new delta is smaller then use that one (enemy turn)
            } else if (color == enemyColor && tempDeltaScoreAllSpheres >= tempDeltaScore) {
                double rand = Math.random();
                if (tempDeltaScoreAllSpheres == tempDeltaScore && rand < 0.5) {
                } else {
                    tempDeltaScoreAllSpheres = tempDeltaScore;
                }
            }
            if(depth==0 && !toPass){
                toRemoveSec=true;
                sphereToMove=tempSphere;
            }


            simulator.undoPass(prevState,color);

            return tempDeltaScoreAllSpheres;

        }else return totalOwnScore - totalEnemyScore;
    }

    private PylosSphere[] getSpheresInSquare(PylosBoard board, PylosPlayerColor color) {
        PylosLocation[] locations = new PylosLocation[4];
        PylosSphere[] spheres = new PylosSphere[4];

        for(PylosSquare square: board.getAllSquares()) {
            if(square.getInSquare(color)==4) {
                locations = square.getLocations();
            }
        }

        int i=0;
        for(PylosLocation location: locations) {
            spheres[i] = location.getSphere();
            i++;
        }

        return spheres;

    }

    //Sam: in de huidige implementatie is er geen zekerheid dat er een bal uit het vierkant genomen wordt
    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        sphereToMove=null;
        locationToMove=null;
        simulator=new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);

        minMaxRecursie(board,0,enemyColor);
        game.removeSphere(sphereToMove);

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        sphereToMove=null;
        locationToMove=null;
        simulator=new PylosGameSimulator(game.getState(),this.PLAYER_COLOR,board);

        minMaxRecursie(board,0,this.PLAYER_COLOR);
        boolean tempRemove = toRemoveSec;
        if(tempRemove){
            toRemoveSec=false;
            game.removeSphere(sphereToMove);
        }
        else {
            game.pass();
        }
    }
/*
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
*/

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

        //fill score map for own spheres
        for (PylosSphere sphere : mySpheres) {
            // If the sphere is still in the reserve, we will not need to calculate a score.
            if (!sphere.isReserve()) {
                newEvaluationFunction(sphere, board,this.PLAYER_COLOR);
            }else {
                scoreMapSpheresOwn.put(sphere, reserveScore);
            }
        }

        int tempvalue=0;
        for(int value : scoreMapSpheresOwn.values()){
            tempvalue += value;
        }
        totalOwnScore=tempvalue;

        //fill score map for enemy spheres
        for (PylosSphere sphere : enemySpheres) {
            // If the sphere is still in the reserve, we will not need to calculate a score.
            if (!sphere.isReserve()) {
                newEvaluationFunction(sphere, board,enemyColor);
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

    //gives a score to a sphere
    /*
    * 3 spheres = 5 * 2 spheres -> 45
    * 2 spheres = 3 * 1 sphere  -> 9
    * 1 sphere = 3 * 0 spheres  -> 3
    * 0 spheres                 -> 1
    * 4 spheres = reservescore  -> 45
    * reservescore = 3 spheres  -> 45
    * height score > 2 * 2s + 2 * 1s -> heightscore = 25
    *
    * */

    private void evaluationFunctionSphere(PylosSphere sphere, PylosBoard board, PylosPlayerColor color) {

        List<PylosSquare> goodSquares = sphere.getLocation().getSquares();

        PylosPlayerColor enemy;
        if(color==this.PLAYER_COLOR)enemy=enemyColor;
        else enemy=this.PLAYER_COLOR;

        // after the squares are found calculate the new score
        int score = 0;

        int enemySpheres;
        int ownSpheres;
        int height = sphere.getLocation().Z;

        //Add the score for each square
        for (PylosSquare square : goodSquares) {
            enemySpheres = square.getInSquare(enemy);
            ownSpheres = square.getInSquare(color);
            switch(ownSpheres){
                case 0:
                    score+=1;
                    break;
                case 1:
                    score+=3;
                    break;
                case 2:
                    score+=9;
                    break;
                case 3 | 4 :
                    score+=45;
                    break;
            }

            switch(enemySpheres){
                case 0:
                    score+=1;
                    break;
                case 1:
                    score+=3;
                    break;
                case 2:
                    score+=9;
                    break;
                case 3 | 4 :
                    score+=45;
                    break;
            }


        }
        score += height * 40;

        scoreMapSpheresOwn.replace(sphere, score);
    }

    private void newEvaluationFunction(PylosSphere sphere, PylosBoard board, PylosPlayerColor color) {

        int scoreForSquare = 100; //x4 moet nog steeds wel kleiner zijn dan reservescore plus wordt 4 keer meegerekend
        int scoreForAlmostSquare = 50; //x3
        int scoreForSabotage = 250; //x1

         /*
                STEP 2: Looking for squares in which the sphere is located
         */
        List<PylosSquare> goodSquares = sphere.getLocation().getSquares();

        // after the squares are found calculate the new score
        int score = 0;

        PylosPlayerColor enemy;
        if(color==this.PLAYER_COLOR)enemy=enemyColor;
        else enemy=this.PLAYER_COLOR;

        int enemySpheres;
        int ownSpheres;
        int height = sphere.getLocation().Z;


        //Add the score for each square
        for (PylosSquare square : goodSquares) {
            enemySpheres = square.getInSquare(enemy);
            ownSpheres = square.getInSquare(color);
            score+=enemySpheres+ownSpheres;

            //Bonus wanneer je bijna een vierkant maakt of effectief een vierkant hebt gemaakt
            if(ownSpheres==4) {
                //Zal vier keer meegeteld worden als bonus
                score+=scoreForSquare;
            }
            else if (ownSpheres==3 && enemySpheres==0) {
                //Zal drie keer meegeteld worden als bonus
                score+=scoreForAlmostSquare;
            }
            else if(ownSpheres==1 && enemySpheres==3) {
                //Zal maar één keer meegeteld worden als bonus
                score+=scoreForSabotage;
            }

        }
        score += height * 20;

        //Midden geven we een beetje bonuspunten want daar lig je meestal beter dus midden midden krijgt 2 puntjes en midden rand krijgt er 1, rand rand krijgt niks
        if(goodSquares.size()!=1) score+=goodSquares.size()/2;

        scoreMapSpheresOwn.replace(sphere, score);
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
/*
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
    }*/
}
