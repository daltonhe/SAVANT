# SAVANT

Change log
10-07:  Started project
        Created position class and Definitions interface
10-08:  Created Move class
10-22:  Finished make and unmake move
        Finished legal move generation and Perft tested, confirmed bug-free
10-23:  Created Engine class
        Created skeleton alpha-beta search
        Added basic material evaluator
        Added iterative deepening framework
        Added PV extraction
        Implemented quiescence search for captures/promotions
        Added basic MVV/LVA sorting scheme
        Removed rook/bishop underpromotions from move generation
        Created basic UI scheme
        Played first online test games (chess24 rating ~1700)
        Added delta pruning to quiescence search
        Implemented aspiration windows
        Last iteration's PV move is now searched first in subsequent iterations
        Moves are now checked for legality during search instead of during move generation to save computation
        Added mobility to evaluation
        Added tempo to evaluation
        Implemented history heuristic
        Implemented principal variation search (PVS)
        Implemented crude repetition detection using FEN history
10-26:  Fixed a bug where quiescence search was generating non-captures/promotions
        Ran Bratko-Kopec test: score=14 (T=9, L=5)
        Implemented basic time control
10-27:  Added basic opening book with ~120 most popular variations from master play
        Cleaned up user interface and added option for engine to play white/black
        Fixed algebraic notation for ambiguous knight/rook moves (e.g. Nbd7)
        Added crude king safety metrics to evaluation
10-28:  Implemented null move pruning
        Added check evasion extension
10-29:  Reached 2350 blitz rating on Lichess (100+ games)
    	  Implemented tapered evaluation
        Added middle and endgame piece-square tables
10-30:  Implemented transposition table with zobrist hashing
        Implemented new repetition detection scheme using small zobrist hash table
   		  PV is now extracted from transposition table following search
   		  Updated mobility evaluation
10-31:  Changed mobility area to exclude blocked pawns, pawns on the 2nd and 3rd ranks, 
   			and king and queen
   		  Updated material values to reflect the ideas in Larry Kaufmann's paper
 	      Added material imbalances (bishop pair bonus, piece redundancy penalties)
   		  Implemented late move reductions (LMR)
11-01:  Changed material values to more closely match Stockfish
 	      Updated piece square table values to match Stockfish
 	      Added major piece redundancy to imbalance evaluation
  		  Simplified mobility evaluation to include all attacked squares not occupied by friendly pieces
  		  Updated mobility bonus values to match Stockfish
  		  Added grain size for more precise eval score
  		  Displayed node count is now cumulative across all search depths
11-02:  Reverted to old material values (Kaufmann 2012)
11-05:  Reverted to Stockfish material values
  		  Split Engine and Evaluate classes
  		  Changed phase weights to roughly match material values (phase is approximately proportional to non-pawn material)
  		  Aspiration window width is now incrementally adjusted 
  		  Fixed a transposition table bug where evals were being stored incorrectly
  		  Fixed a transposition table bug concerning faulty ply values as result of check extensions
  		  Added various material imbalance bonuses to evaluation
11-06:  Fixed a transposition table bug where TT values were being erroneously used at PV nodes
  		  Implemented mate distance pruning
  	    Null move, LMR, and TT cutoffs are now only used in non-PV nodes
  		  Node count is no longer incremented in quiescence search
 	      Pawn moves to the 7th rank are now checked in quiescence search
  		  Fixed a bug where castling moves were being generated during quiescence search
  		  Check extensions are no longer applied at the root node
  		  Added passed pawn evaluation
  		  Added doubled and isolated pawn penalties to evaluation
  		  Added bonuses for rook on open/semi-open file
11-07:  Added bonuses for connected pawns
  		  Reverted to old material values (Kaufmann 2012) and old phase values (Fruit)
  		  Tweaked imbalance evaluation
  		  Reached 2400 blitz and 2250 bullet rating on Lichess
  		  Added penalty for pawns on the same color square as the bishop
  		  Added penalties for trapped bishop and trapped rook
  		  Added bonuses for rook and queen on the 7th rank
  		  Tempo bonus is no longer applied to endgame score
  		  Put a limit on the number of plies the search can be extended
  		  Added insufficient material check to search
  		  Opened GitHub repository
  	    Fixed a bug involving faulty repetition detection when null moving
  		  Fixed a bug where zobrist keys were initialized incorrectly. This was the actual root cause of many hash table bugs which I                thought I addressed earlier.
        Added game over handling to UI
        Reached 2300 bullet rating on Lichess
        Reached 2450 blitz rating on Lichess (account was finally flagged for computer assistance)
11-09:  Added more efficient zobrist key updating during makeMove() and unmakeMove()
        Reinstated underpromotions to move generation
        Changed castling rights storage from string to int
        Revalidated perft and sanity checked hash table
        Added node type parameter to search and made corrections to PVS implementation
        Added special evaluator for use in mating with KX vs K
11-10:  Added UCI support
        Fixed a bug where getMoveObject() failed to recognize promotion inputs
11-11:  Changed formula for calculating aspiration windows
        Search is now terminated prematurely when there is only one legal move
11-12:  Added time control support to UCI
        Search is now hard stopped when time is up
        Implemented internal iterative deepening (IID)
        Added static evaluation restriction to null move pruning
        Added dynamic LMR reduction factor based on node type, ply, and moves searched
        Endgame eval score is now scaled down when a draw due to insufficient material is likely
        Zobrist keys are now the same irrespective of the side to move for repetition detection
11-13:  Added bonuses for king proximity to passed pawns in the endgame
11-14:  Implemented in-tree repetition detection
        Repetition hash table is now stored as a field in Position class
        Moved null move parameters to Position class
        Fixed a bug where the inCheck flag was set incorrectly during search
        Fixed a bug where delta pruning margin was incorrect for promotions
        Delta pruning is now switched off in the late endgame (this detection is hacky however)
        Implemented futility pruning and extended futility pruning
        Changed hash table sizes to prime numbers
        Added replace-by-age scheme to transposition table; TT is no longer cleared between searches
        PV hash table is now cleared between searches
        Implemented limited razoring
        Added penalty for backward pawns
        More time is now allocated for early moves
        Search is now terminated prematurely when the time used for the last depth is more than half the allocated time for this move
        Fixed a bug in which UCI PV lines were being sent using the wrong format
        Removed bonus to rook value for having more pawns
11-15:  Tested Stockfish material values and reverted to them
        Added lazy eval for positions with very large material difference 
        Added separate transposition table class
        Fixed several issues related to repetition draw scores and TT; a special value is now used to distinguish path-dependent
           draw evaluations (those arising from 3-fold repetition and 50 moves rule)
