package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;

public class StudentPlayerBestFit extends PylosPlayer{

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

    // globale map: key= sphere , value = score // optioneel: 2 scores, verliezend of winnend
    // globale Board opslaan, en iedere locatie vergelijkt met de vorige -> nieuwe scores
    // evaluatie fctie: vierkant, eigen ballen in square viscinity, enemy ballen in square viscinity, hoe lager de bal; hoe lager de score
    // eigen vierkant of enemy vierkant? -> hangt ervan af hoeveel ballen in reserve zijn bij ons en de enemy
    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
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
}
