package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentPlayerBestFit extends PylosPlayer {

    private Map<PylosSphere, Integer> scoreMapSpheresOwn;
    private Map<PylosSphere, Integer> scoreMapSpheresEnemy;
    private boolean initialized = false;
    private PylosBoard previousBoard;
    private PylosPlayerColor enemyColor;
    private PylosGameSimulator simulator;

    private final int valueHeightInLocationScore = 20;
    private int reserveScore = 750;

    private int totalOwnScore = 0;
    private int totalEnemyScore = 0;

    //recursiondepth verhogen zorgt voor een beter resultaat, maar dan duren de berekeningen langer
    private final int recursionDepth = 12;
    private final int MaxNumberOfProbableMoves = 15;

    private PylosSphere sphereToMove=null;
    private PylosLocation locationToMove=null;
    private boolean toRemoveSec = false;


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

        game.moveSphere(sphereToMove,locationToMove);

    }


    private int minMaxRecursie(PylosBoard board, int depth,PylosPlayerColor color){
        PylosGameState currentState=simulator.getState();

        PylosGameState prevState=simulator.getState();
        PylosPlayerColor nextColor;
        int tempWinner=Integer.MIN_VALUE;;
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

            int bestScore = probableScores[0];
            int bestScoreIndex = 0;

            for (PylosSphere sphere : usableSpheres) {
                for (PylosLocation location : usableLocations) {
                    if(sphere.canMoveTo(location)) {

                        boolean isReserve = sphere.isReserve();

                        PylosLocation prevLoc = sphere.getLocation();
                        prevState = simulator.getState();

                        simulator.moveSphere(sphere, location);
                        calculateAllScores(board);

                        int tempDeltaScore = totalOwnScore - totalEnemyScore; //moet maximaal zijn
                        if((color==this.PLAYER_COLOR && tempDeltaScore >= bestScore) || (color==enemyColor && tempDeltaScore <= bestScore)) {

                            probableSpheres[bestScoreIndex] = sphere;
                            probableLocations[bestScoreIndex] = location;
                            probableScores[bestScoreIndex] = tempDeltaScore;
                            bestScore = tempDeltaScore;

                            for (int i = 0; i < MaxNumberOfProbableMoves; i++) {
                                if (color==this.PLAYER_COLOR && bestScore <= probableScores[i]) {
                                    bestScore = probableScores[i];
                                    bestScoreIndex = i;
                                }
                                else if(color==enemyColor && bestScore >= probableScores[i]) {
                                    bestScore = probableScores[i];
                                    bestScoreIndex = i;
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

            for(int i=0;i<MaxNumberOfProbableMoves;i++) {
                PylosSphere sphere = probableSpheres[i];
                PylosLocation location = probableLocations[i];
                if (sphere != null && location != null && sphere.canMoveTo(location)) {
                    boolean isReserve = false;
                    if (sphere.isReserve()) isReserve = true;

                    PylosLocation prevLoc = sphere.getLocation();
                    prevState = simulator.getState();

                    simulator.moveSphere(sphere, location);
                    if(simulator.getState() == PylosGameState.COMPLETED && depth != 0) {
                        PylosPlayerColor winner = simulator.getWinner();

                        if (isReserve) simulator.undoAddSphere(sphere, prevState, color);
                        else simulator.undoMoveSphere(sphere, prevLoc, prevState, color);

                        if (winner == this.PLAYER_COLOR) {
                            return Integer.MAX_VALUE;
                        } else return Integer.MIN_VALUE;
                    }

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

                    if(tempDeltaScore == Integer.MAX_VALUE){
                        if(depth == 0) {
                            if( tempWinner <= totalOwnScore) {
                                double rand = Math.random();
                                if (tempWinner == totalOwnScore && rand < 0.5) {
                                } else {
                                    tempWinner = totalOwnScore;
                                    tempSphere = sphere;
                                    tempLocation = location;
                                }
                            }
                        }else{
                            if (isReserve) simulator.undoAddSphere(sphere, prevState, color);
                            else simulator.undoMoveSphere(sphere, prevLoc, prevState, color);
                            return Integer.MAX_VALUE;
                        }
                    }else if (tempDeltaScore == Integer.MIN_VALUE){
                        if(depth ==0 && tempSphere == null){
                            if( tempWinner <= totalOwnScore) {
                                double rand = Math.random();
                                if (tempWinner == totalOwnScore && rand < 0.5) {
                                } else {
                                    tempWinner = totalOwnScore;
                                    tempSphere = sphere;
                                    tempLocation = location;
                                }
                            }
                        }else{
                        if (isReserve) simulator.undoAddSphere(sphere, prevState, color);
                        else simulator.undoMoveSphere(sphere, prevLoc, prevState, color);

                        return Integer.MIN_VALUE;
                        }
                    }
                    //if new delta is bigger then use that one (own turn)
                    else if (color == this.PLAYER_COLOR && tempDeltaScoreAllSpheres <= tempDeltaScore) {
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

            if(depth == 0) {
                sphereToMove = tempSphere;
                locationToMove = tempLocation;
            }


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
             First delete will always take a sphere from the made square, hard coded want anders kon ons scoresysteem hiermee niet overweg.
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

            if(depth == 0 && sphereToMove == null){
                System.out.print("sdfs");
            }

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


    // initializing the map that contains the score per sphere and save the first board
    private void initializeScore(PylosGameIF game, PylosBoard board) {

        //Checking what the color of the enemy is
        if (this.PLAYER_COLOR == PylosPlayerColor.LIGHT) {
            enemyColor = PylosPlayerColor.DARK;
        } else enemyColor = PylosPlayerColor.LIGHT;

        scoreMapSpheresOwn = new HashMap<>();
        scoreMapSpheresEnemy = new HashMap<>();
        simulator = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR,board);

        PylosSphere[] mySpheres = board.getSpheres(this);
        PylosSphere[] enemySpheres = board.getSpheres(enemyColor);

        for (PylosSphere sphere : mySpheres) {
            scoreMapSpheresOwn.put(sphere, reserveScore);
        }

        for (PylosSphere sphere : enemySpheres) {
            scoreMapSpheresEnemy.put(sphere, reserveScore);
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
                evaluationFunction(sphere, board,this.PLAYER_COLOR);
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
                evaluationFunction(sphere, board,enemyColor);
            } else {
                scoreMapSpheresEnemy.put(sphere, reserveScore);
            }
        }

        tempvalue=0;
        for(int value : scoreMapSpheresEnemy.values()){
            tempvalue += value;
        }
        totalEnemyScore=tempvalue;

    }

    public int exponent(int base, int exp){
        int result=1;
        for(int i=0;i<exp;i++){
            result = result*base;
        }

        return result;
    }



    private void evaluationFunction(PylosSphere sphere, PylosBoard board, PylosPlayerColor color) {

        int scoreForSquare = 100; //x4 moet nog steeds wel kleiner zijn dan reservescore plus wordt 4 keer meegerekend
        int scoreForAlmostSquare = 50; //x3
        int scoreForSabotage = reserveScore+1; //x1

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
                //Zal maar één keer meegeteld worden als bonus, we nemen reservescore omdat de totale score van de sphere (plus hoogte enz) sws groter is dan de reservescore
                score+=scoreForSabotage;
            }

        }
        score += height * valueHeightInLocationScore;

        //Midden geven we een beetje bonuspunten want daar lig je meestal beter dus midden midden krijgt 2 puntjes en midden rand krijgt er 1, rand rand krijgt niks
        if(goodSquares.size()!=1) score+=goodSquares.size()/2;

        if(color==this.PLAYER_COLOR)scoreMapSpheresOwn.replace(sphere, score);
        else scoreMapSpheresEnemy.replace(sphere, score);
    }


}
