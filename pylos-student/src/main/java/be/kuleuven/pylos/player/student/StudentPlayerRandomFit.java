package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGameIF;
import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosSphere;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Ine on 5/05/2015.
 */
public class StudentPlayerRandomFit extends PylosPlayer {

    public PylosLocation getRandomFeasibleLocation(PylosGameIF game, PylosBoard board) {
        List<PylosLocation> options = new ArrayList<>();
        for (PylosLocation pl : board.getLocations()) {
            if (board.getReserve(this).canMoveTo(pl)) {
                options.add(pl);
            }
        }
        int random = (int) Math.random() * options.size();
        return options.get(random);
    }

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        /* add a reserve sphere to a feasible random location */
        PylosLocation pl = getRandomFeasibleLocation(game, board);
        game.moveSphere(board.getReserve(this), pl);
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        /* removeSphere a random sphere */
        PylosSphere sphere = randomFeasibleRemove(board);
        game.removeSphere(sphere);
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        /* always pass */
        game.pass();
    }

    private PylosSphere randomFeasibleRemove(PylosBoard board){

        PylosSphere[] mySpheres = board.getSpheres(this);
        List<PylosSphere> removableSpheres = new ArrayList<>();

        //check each sphere that is on the board if it can be removed
        for(PylosSphere sphere: mySpheres){
            if(sphere.canRemove())removableSpheres.add(sphere);
        }
        //choose a random removable sphere
        int random = (int) Math.random() * removableSpheres.size();

        return removableSpheres.get(random);
    }
}
