package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentPlayerBestFit extends PylosPlayer{

    private Map< PylosSphere, Integer > scoreMap;
    private boolean initialized=false;
    private PylosBoard previousBoard;
    private PylosPlayerColor enemyColor;

    private final int reserveScore = 80;

    // check** globale map: key= sphere , value = score // optioneel: 2 scores, verliezend of winnend
    // check** (ben niet zeker of dit nodig is)+(java gebruikt pass by copy bij argumenten) globale Board opslaan, en iedere locatie vergelijkt met de vorige -> nieuwe scores
    // evaluatie fctie: vierkant, eigen ballen in square viscinity, enemy ballen in square viscinity, hoe lager de bal; hoe lager de score
    // eigen vierkant of enemy vierkant? -> hangt ervan af hoeveel ballen in reserve zijn bij ons en de enemy

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        //Initialize method will have saved the previous board and checked which color this player is + added spheres to map
        if (!initialized)initializeScore(board);
        calculateAllScores(board);

        //for insurance that we always move a sphere
        PylosSphere movingSphere = board.getReserve(this);
        PylosLocation movingLocation = null;

        //step 1
        //Check if we are losing or winning
        boolean winning = false;
        if(board.getReservesSize(this) >= board.getReservesSize(enemyColor))winning=true;

        if(winning){
            //check 1 : can we make our own square?
            List<PylosSquare> squares = getPossibleCompleteOwnSquares(board);
            if(!squares.isEmpty()){

                // step 1 : can we make a square by moving a ball on the board?
                PylosSphere tempSphere = movingSphere;
                PylosLocation tempLocation = movingLocation;

                for(PylosSquare square: squares){
                    for(PylosLocation location : square.getLocations()){
                        tempSphere = squareOnBoardMovable(board, location);
                        if(tempSphere != null) tempLocation = location;
                    }
                    if(tempSphere != null) {
                        if (scoreMap.get(tempSphere) < scoreMap.get(movingSphere)) {
                            movingSphere = tempSphere;
                            movingLocation = tempLocation;
                        }
                    }
                }

                //step 2 : we fill a random square
                for(PylosLocation location : squares.get(0).getLocations()){
                    if(movingSphere.canMoveTo(location))movingLocation=location;
                }

                game.moveSphere(movingSphere, movingLocation);

            }
            //check 2 : can we undermine an enemy square
            else {
                squares = getPossibleCompleteEnemySquares(board);
                if(!squares.isEmpty()){

                }
            }
        }
        //if we are not winning prioritize the undermining the enemy
        else {

        }
        //3 onderdelen : 1- kijken of we aan het winnen zijn 2- zo ja is er een bol die al op het veld ligt en naar boven kan 3- anders een uit reserve


        //Stap 1: check als je een vierkant kan maken
        PylosLocation pl = getLocationToMakeSquare(game, board);
        if(pl!=null) {
            //TODO: als je een bol van een lager niveau kan plaatsen naar een square op een hoger niveau
            //TODO 2.0: kijken als je allebei een vierkant kan maken: wie wint en saboteer je eerst of niet?
            game.moveSphere(board.getReserve(this),pl);
        }

        //Stap 2: blokkeer als de ander een vierkant kan maken (door middel van een bol van lager niveau naar hoger niveau)

        //Stap 3: check als je een bol kan verplaatsen naar een niveau hoger (zonder dat de andere dan een square kan maken)

        //Stap 4: zet een bol zodanig dat jij de keer erna een vierkant kan maken (of bijna)

    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        calculateAllScores(board);

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {

    }

    private PylosSphere squareOnBoardMovable(PylosBoard board, PylosLocation location){
        PylosSphere movingSphere = null;
        for (PylosSphere sphere : board.getSpheres(this)){
            if(!sphere.isReserve() && sphere.canMoveTo(location)){
                if(movingSphere != null){
                    if(scoreMap.get(sphere) < scoreMap.get(movingSphere)){
                        movingSphere=sphere;
                    }
                }else{
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

    private PylosLocation getLocationToMakeSquare(PylosGameIF game, PylosBoard board) {
        for(PylosSquare ps: board.getAllSquares()) {
            if(ps.getInSquare(this) == 3) {
                for(PylosLocation pl: ps.getLocations()) {
                    if(board.getReserve(this).canMoveTo(pl)) return pl;
                }
            }
        }
        return null;
    }

    // initializing the map that contains the score per sphere and save the first board
    private void initializeScore(PylosBoard board){

        scoreMap = new HashMap<>();
        PylosSphere[] mySpheres = board.getSpheres(this);

        for(PylosSphere sphere: mySpheres){
            scoreMap.put(sphere,reserveScore);
        }

        //Checking what the color of the enemy is
        if(this.PLAYER_COLOR == PylosPlayerColor.LIGHT){
            enemyColor=PylosPlayerColor.DARK;
        }else enemyColor=PylosPlayerColor.LIGHT;

        previousBoard = board;
        initialized = true;
    }

    //calculate the new scores for all our spheres on the board
    private void calculateAllScores(PylosBoard board){
        PylosSphere[] mySpheres = board.getSpheres(this);
        for(PylosSphere sphere : mySpheres){
            evaluationFunction(sphere,board);
        }
    }

    /*gives a score to a sphere
    higher score = better placement

    reserve = 85        // min score is 50

    own spheres --> (n-1)*(n-1)*15 + 30 ... gebruik mss toch geen algemene formule wegens de reden gegeven bij 3 spheres
            Vergeet niet: uw eigen bol wordt in uw redenering ook meegeteld als ownSpheres maw opties zijn 1,2,3,4

    0 own spheres (+this) = 30
    1 own spheres (+this) = 45
    2 own spheres (+this) = 90
    3 own spheres (+this) = 165 -> Dit moet 0 zijn. Het is juist goed om die weg te halen omdat je dan mss nog eens een kans hebt om dat vierkant opnieuw te maken!

    enemy spheres --> n*n*15 + 20

    0 enemy spheres = 20
    1 enemy spheres = 35
    2 enemy spheres = 80
    3 enemy spheres = 155

    height bal = z * 30          z is goed want we willen liever een bal van de onderste laag naar een bovenste leggen dan eentje halen uit reserve dus geen extra punten voor eerste layer
     */
    private void evaluationFunction(PylosSphere sphere, PylosBoard board){

        /*
                STEP 1: If the sphere is still in the reserve, we will not need to calculate a score.
         */
        if(sphere.isReserve()) {
            scoreMap.replace(sphere , reserveScore);
            return;
        }

        /*
                STEP 2: Looking for squares in which the sphere is located
         */
        List<PylosSquare> goodSquares = new ArrayList<>();

        //searches the squares where the sphere is in; min 1 , max 4
        //TODO: mogelijke verbetering qua snelheid: je checkt van elke square maar 1 locatie en pas als die max 1 verschilt qua coordinaten met eigen locatie, kijk je naar de rest. Gaat wel enkel beter zijn bij grotere spelborden
        for(PylosSquare square : board.getAllSquares()){
            for(PylosLocation location : square.getLocations()){
                if(sphere.getLocation() == location){
                    goodSquares.add(square);
                    break;
                }
            }
            if(goodSquares.size()==4)break;
        }

        // after the squares are found calculate the new score
        int score = 0;

        int enemySpheres;
        int ownSpheres;
        int height = sphere.getLocation().Z;

        //Add the score for each square
        for(PylosSquare square : goodSquares){
            enemySpheres = square.getInSquare(enemyColor);
            ownSpheres = square.getInSquare(this.PLAYER_COLOR);
            score += (enemySpheres * enemySpheres * 15 +20);
            //If ownSpheres is 4, there already is a square of own color so it would be best to remove this sphere if possible in order to remake the square
            if(ownSpheres!=4) {
                score += (ownSpheres-1) * (ownSpheres-1) * 15 + 30;
            }

        }
        score += height * 30;

        scoreMap.replace(sphere , score);

    }
}
