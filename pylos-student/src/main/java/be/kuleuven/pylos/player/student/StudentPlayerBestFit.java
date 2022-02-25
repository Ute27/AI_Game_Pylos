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

    // check** globale map: key= sphere , value = score // optioneel: 2 scores, verliezend of winnend
    // check** (ben niet zeker of dit nodig is)+(java gebruikt pass by copy bij argumenten) globale Board opslaan, en iedere locatie vergelijkt met de vorige -> nieuwe scores
    // evaluatie fctie: vierkant, eigen ballen in square viscinity, enemy ballen in square viscinity, hoe lager de bal; hoe lager de score
    // eigen vierkant of enemy vierkant? -> hangt ervan af hoeveel ballen in reserve zijn bij ons en de enemy

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        if (!initialized)initializeScore(board);

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

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {

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

        int reserveScore = 30;

        scoreMap = new HashMap<>();
        PylosSphere[] mySpheres = board.getSpheres(this);

        for(PylosSphere sphere: mySpheres){
            scoreMap.put(sphere,reserveScore);
        }

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

    reserve = 30

    own spheres --> n*n*15 + 30

    0 own spheres = 30
    1 own spheres = 45
    2 own spheres = 90
    3 own spheres = 165

    enemy spheres --> n*n*15 + 20

    0 enemy spheres = 20
    1 enemy spheres = 35
    2 enemy spheres = 80
    3 enemy spheres = 155

    height bal = z * 30          (kan mis (z+1)*30 worden)
     */
    private void evaluationFunction(PylosSphere sphere, PylosBoard board){
        int foundSquares = 0;
        List<PylosSquare> goodSquares = new ArrayList<>();

        //searches the squares where the sphere is in; min 1 , max 4
        for(PylosSquare square : board.getAllSquares()){
            for(PylosLocation location : square.getLocations()){
                if(sphere.getLocation() == location){
                    foundSquares++;
                    goodSquares.add(square);
                    break;
                }
            }
            if(foundSquares==4)break;
        }

        // after the squares are found calculate the new score
        int score = 0;

        int enemySpheres;
        int ownSpheres;
        int height = sphere.getLocation().Z;

        //Checking what the color of the enemy is
        PylosPlayerColor enemyColor;
        if(this.PLAYER_COLOR == PylosPlayerColor.LIGHT){
            enemyColor=PylosPlayerColor.DARK;
        }else enemyColor=PylosPlayerColor.LIGHT;

        //Add the score for each square
        for(PylosSquare square : goodSquares){
            enemySpheres = square.getInSquare(enemyColor);
            ownSpheres = square.getInSquare(this.PLAYER_COLOR);
            score += (enemySpheres * enemySpheres +20) + (ownSpheres * ownSpheres + 30);
        }
        score += height * 30;

        scoreMap.replace(sphere , score);

    }
}
