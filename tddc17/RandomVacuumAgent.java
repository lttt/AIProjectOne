package tddc17;

import aima.core.environment.liuvacuum.*;
import aima.core.agent.Action;
import aima.core.agent.AgentProgram;
import aima.core.agent.Percept;
import aima.core.agent.impl.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

//DSF
class DsfAgentState {
	public int[][] world = new int[30][30];
	public int initialized = 0;
	final int UNKNOWN = 0;
	final int WALL = 1;
	final int CLEAR = 2;
	final int DIRT = 3;
	final int HOME = 4;
	final int ACTION_NONE = 0;
	final int ACTION_MOVE_FORWARD = 1;
	final int ACTION_TURN_RIGHT = 2;
	final int ACTION_TURN_LEFT = 3;
	final int ACTION_SUCK = 4;

	public int agent_x_position = 1;
	public int agent_y_position = 1;
	public int agent_last_action = ACTION_NONE;

	public static final int NORTH = 0;
	public static final int EAST = 1;
	public static final int SOUTH = 2;
	public static final int WEST = 3;
	public int agent_direction = EAST;
	public int tmp_direction = EAST;

	// Agent don't know about the environment when initialing
	DsfAgentState() {
		for (int i = 0; i < world.length; i++)
			for (int j = 0; j < world[i].length; j++)
				world[i][j] = UNKNOWN;
		world[1][1] = HOME;
		agent_last_action = ACTION_NONE;
	}

	// Based on the last action and the received percept updates the x & y agent
	// position
	public void updatePosition(DynamicPercept p) {
		Boolean bump = (Boolean) p.getAttribute("bump");

		if (agent_last_action == ACTION_MOVE_FORWARD && !bump) {
			switch (agent_direction) {
			case DsfAgentState.NORTH:
				agent_y_position--;
				break;
			case DsfAgentState.EAST:
				agent_x_position++;
				break;
			case DsfAgentState.SOUTH:
				agent_y_position++;
				break;
			case DsfAgentState.WEST:
				agent_x_position--;
				break;
			}
		}

	}

	public void updateDirection(DynamicPercept p) {

		if (agent_last_action == ACTION_TURN_RIGHT) {
			switch (agent_direction) {
			case DsfAgentState.NORTH:
				agent_direction = DsfAgentState.EAST;
				break;
			case DsfAgentState.EAST:
				agent_direction = DsfAgentState.SOUTH;
				break;
			case DsfAgentState.SOUTH:
				agent_direction = DsfAgentState.WEST;
				break;
			case DsfAgentState.WEST:
				agent_direction = DsfAgentState.NORTH;
				break;
			}
		}
		if (agent_last_action == ACTION_TURN_LEFT) {
			switch (agent_direction) {
			case DsfAgentState.NORTH:
				agent_direction = DsfAgentState.WEST;
				break;
			case DsfAgentState.EAST:
				agent_direction = DsfAgentState.NORTH;
				break;
			case DsfAgentState.SOUTH:
				agent_direction = DsfAgentState.EAST;
				break;
			case DsfAgentState.WEST:
				agent_direction = DsfAgentState.SOUTH;
				break;
			}
		}

	}

	public void updateWorld(int x_position, int y_position, int info) {
		world[x_position][y_position] = info;
	}

	public void printWorldDebug() {
		for (int i = 0; i < world.length; i++) {
			for (int j = 0; j < world[i].length; j++) {
				if (world[j][i] == UNKNOWN)
					System.out.print(" ? ");
				if (world[j][i] == WALL)
					System.out.print(" # ");
				if (world[j][i] == CLEAR)
					System.out.print(" c ");
				if (world[j][i] == DIRT)
					System.out.print(" D ");
				if (world[j][i] == HOME)
					System.out.print(" H ");
			}
			System.out.println("");
		}
	}
}

class DsfAgentProgram implements AgentProgram {

	private int initnialRandomActions = 5;
	private Random random_generator = new Random();

	// TODO Here you can define your variables!
	public int iterationCounter = 1000;
	public DsfAgentState state = new DsfAgentState();

	// Planed Action Sequence
	public Queue<Action> traceActions = new LinkedList<Action>();
	// dfs Stack
	public Stack<Node> dfsStack = new Stack<Node>();
	// Queued Set
	private Set<Node> visitiedNode = new HashSet<Node>();

	public Map<Action, Integer> actionMap = new HashMap<Action, Integer>() {
		{
			put(LIUVacuumEnvironment.ACTION_MOVE_FORWARD, state.ACTION_MOVE_FORWARD);
			put(LIUVacuumEnvironment.ACTION_SUCK, state.ACTION_SUCK);
			put(LIUVacuumEnvironment.ACTION_TURN_LEFT, state.ACTION_TURN_LEFT);
			put(LIUVacuumEnvironment.ACTION_TURN_RIGHT, state.ACTION_TURN_RIGHT);

		}
	};

	// home Position
	public Node homePostion;

	// moves the Agent to a random start position
	// uses percepts to update the Agent position - only the position, other
	// percepts are ignored
	// returns a random action
	private Action moveToRandomStartPosition(DynamicPercept percept) {
		int action = random_generator.nextInt(6);
		initnialRandomActions--;
		state.updatePosition(percept);
		if (action == 0) {
			state.agent_direction = ((state.agent_direction - 1) % 4);
			if (state.agent_direction < 0)
				state.agent_direction += 4;
			state.agent_last_action = state.ACTION_TURN_LEFT;
			return LIUVacuumEnvironment.ACTION_TURN_LEFT;
		} else if (action == 1) {
			state.agent_direction = ((state.agent_direction + 1) % 4);
			state.agent_last_action = state.ACTION_TURN_RIGHT;
			return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
		}
		state.agent_last_action = state.ACTION_MOVE_FORWARD;
		return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	}

	/*
	 * (return value) is one of the following actions:
	 * 
	 * LIUVacuumEnvironment.ACTION_MOVE_FORWARD moves the agent one step forward
	 * depending on its direction.
	 * 
	 * LIUVacuumEnvironment.ACTION_TURN_LEFT makes the agent turn left.
	 * LIUVacuumEnvironment.ACTION_TURN_RIGHT makes the agent turn right.
	 * 
	 * LIUVacuumEnvironment.ACTION_SUCK cleans the dirt from the agent's current
	 * location.
	 * 
	 * NoOpAction.NO_OP informs the simulator that the agent has finished cleaning
	 */

	@Override
	public Action execute(Percept percept) {

		// DO NOT REMOVE this if condition!!!
		if (initnialRandomActions > 0) {
			return moveToRandomStartPosition((DynamicPercept) percept);
		} else if (initnialRandomActions == 0) {
			// process percept for the last step of the initial random actions
			initnialRandomActions--;
			state.updatePosition((DynamicPercept) percept);
			// ADD THE FIRST NODE TO STACK
			Node root = new Node(state.agent_x_position, state.agent_y_position);
			dfsStack.push(root);
			visitiedNode.add(root);
			System.out.println("Processing percepts after the last execution of moveToRandomStartPosition()");
			state.agent_last_action = state.ACTION_SUCK;
			return LIUVacuumEnvironment.ACTION_SUCK;
		}

		System.out.println("x=" + state.agent_x_position);
		System.out.println("y=" + state.agent_y_position);
		System.out.println("dir=" + state.agent_direction);

		if (iterationCounter-- == 0)
			return NoOpAction.NO_OP;

		DynamicPercept p = (DynamicPercept) percept;
		Boolean bump = (Boolean) p.getAttribute("bump");
		Boolean dirt = (Boolean) p.getAttribute("dirt");
		Boolean home = (Boolean) p.getAttribute("home");
		System.out.println("percept: " + p); // and delete this

		// State update based on the percept value and the last action

		state.updatePosition((DynamicPercept) percept);
		state.updateDirection((DynamicPercept) percept);

		if (!traceActions.isEmpty()) {
			// means that we are at a tracing back action
			Action nextaction = traceActions.poll();
			state.agent_last_action = actionMap.get(nextaction);
			return nextaction;
		}
		if (dfsStack.isEmpty())
			return NoOpAction.NO_OP;

		if (state.world[state.agent_x_position][state.agent_y_position] == state.UNKNOWN
				|| state.world[state.agent_x_position][state.agent_y_position] == state.HOME) {

			// all sub nodes in queue( from current direction
			// THE FIRST NODE TO QUEUE
			if (state.agent_x_position + 1 < state.world.length) {
				Node node = new Node(state.agent_x_position + 1, state.agent_y_position);
				if (!visitiedNode.contains(node)) {
					dfsStack.push(node);
					visitiedNode.add(node);
				}
			}
			if (state.agent_x_position - 1 >= 0) {
				Node node = new Node(state.agent_x_position - 1, state.agent_y_position);
				if (!visitiedNode.contains(node)) {
					dfsStack.push(node);
					visitiedNode.add(node);
				}
			}
			if (state.agent_y_position + 1 < state.world[0].length) {
				Node node = new Node(state.agent_x_position, state.agent_y_position + 1);
				if (!visitiedNode.contains(node)) {
					dfsStack.push(node);
					visitiedNode.add(node);
				}
			}
			if (state.agent_y_position - 1 >= 0) {
				Node node = new Node(state.agent_x_position, state.agent_y_position - 1);
				if (!visitiedNode.contains(node)) {
					dfsStack.push(node);
					visitiedNode.add(node);
				}
			}

		}

		if (bump) {
			switch (state.agent_direction) {
			case DsfAgentState.NORTH:
				state.updateWorld(state.agent_x_position, state.agent_y_position - 1, state.WALL);
				break;
			case DsfAgentState.EAST:
				state.updateWorld(state.agent_x_position + 1, state.agent_y_position, state.WALL);
				break;
			case DsfAgentState.SOUTH:
				state.updateWorld(state.agent_x_position, state.agent_y_position + 1, state.WALL);
				break;
			case DsfAgentState.WEST:
				state.updateWorld(state.agent_x_position - 1, state.agent_y_position, state.WALL);
				break;
			}
		}
		if (dirt)
			state.updateWorld(state.agent_x_position, state.agent_y_position, state.DIRT);
		else if (!home)// don't uodate home
			state.updateWorld(state.agent_x_position, state.agent_y_position, state.CLEAR);
		if (home && homePostion == null)
			homePostion = new Node(state.agent_x_position, state.agent_y_position);

		state.printWorldDebug();

		// Next action selection based on the percept value
		// keep moving forward untill hits a bump
		if (dirt) {
			System.out.println("DIRT -> choosing SUCK action!");
			state.agent_last_action = state.ACTION_SUCK;
			return LIUVacuumEnvironment.ACTION_SUCK;
		} else {
			/*
			 * 1. depth first
			 */
			if (bump) {
				Node now = new Node(state.agent_x_position, state.agent_y_position);
				Node dest = now;

				// next node that's unkown
				while (!dfsStack.isEmpty()) {
					Node n = dfsStack.peek();// the dest
					// dont go back to the spot that agent already know
					if (state.world[n.x_position][n.y_position] == state.WALL
							|| state.world[n.x_position][n.y_position] == state.CLEAR
							|| now.equals(n)) {
						dfsStack.pop();
					} else {
						dest = n;
						break;
					}

				}

				if (now.equals(dest)) {// no nodes unknown , go home
					traceActions = shortestPathAction(now, homePostion);
					if (null==traceActions){
						return NoOpAction.NO_OP; 
					}
					state.agent_last_action = actionMap.get(traceActions.peek());
					return traceActions.poll();

				}
				traceActions = shortestPathAction(now, dest);
				// check if that is the position that forward to, if not throw exception
				// find the shortest way to next node tin the Queue: another BFS
				// return the next action

				state.agent_last_action = actionMap.get(traceActions.peek());
				return traceActions.poll();
			} else {
				Node now = new Node(state.agent_x_position, state.agent_y_position);
				Node dest = now;
				// next node that's unkown
				while (!dfsStack.isEmpty()) {
					Node n = dfsStack.peek();// the dest
					// dont go back to the spot that agent already know
					if (state.world[n.x_position][n.y_position] == state.WALL
							|| state.world[n.x_position][n.y_position] == state.CLEAR
							|| now.equals(n))  {
						dfsStack.pop();
					} else {
						dest = n;
						break;
					}

				}
				if (now.equals(dest)) {// no nodes unknown , go home
					traceActions = shortestPathAction(now, homePostion);
					if (null==traceActions){
						return NoOpAction.NO_OP; 
					}
					state.agent_last_action = actionMap.get(traceActions.peek());
					return traceActions.poll();
				}

				traceActions = shortestPathAction(now, dest);// to child node
				// not move forward but to the first in queue
				state.agent_last_action = actionMap.get(traceActions.peek());
				return traceActions.poll();

			}
		}
	}

	// private Queue<Node> nodeQueue;
	// private Set<Node> visited;
	// only using the node we know
	private Queue<Action> shortestPathAction(Node from, Node dest) {

		if (from == null || dest == null) {
			return null;
		}
		if (from.equals(dest)) {
			return null;
		}

		Set<Node> visited = new HashSet<Node>();
		Queue<Node> nodeQueue = new LinkedList<Node>();
	      // from dest to start, through all the nodes we known
		visited.add(dest);
		nodeQueue.offer(dest);
		Node current = null;
		while (!nodeQueue.isEmpty()) {
			current = nodeQueue.poll();
			if (from.equals(current)) {
				break;
			}
			List<Node> neighbor = getQualifiedNeighbor(current);
			for (Node n : neighbor) {
				if (!visited.contains(n)) {
					visited.add(n);
					n.parent = current;
					nodeQueue.offer(n);
				}

			}

		}
		if (current == null) {
			return null;
		} else {
			Queue<Action> actions = new LinkedList<Action>();
			// the way from ——》 dest
			state.tmp_direction = state.agent_direction;
			while (current.parent != null) {
				Queue<Action> alist = shortestActionsForNeighbor(current, current.parent);
				while (alist.peek() != null) {
					actions.offer(alist.poll());
				}
				current = current.parent;

			}
			return actions;
		}

	}
	
	// private Queue<Node> nodeQueue;
	// private Set<Node> visited;
	// only using the node we know
	// iterating all the shortest path, searching for the minimum actions
	private Queue<Action> shortestPathActionV2(Node from, Node dest) {

		if (from == null || dest == null) {
			return null;
		}
		if (from.equals(dest)) {
			return null;
		}

		Set<Node> visited = new HashSet<Node>();
		Queue<Node> nodeQueue = new LinkedList<Node>();
	      // from dest to start, through all the nodes we known
		visited.add(dest);
		nodeQueue.offer(dest);
		Node current = null;
		while (!nodeQueue.isEmpty()) {
			current = nodeQueue.poll();
			if (from.equals(current)) {
				break;
			}
			List<Node> neighbor = getQualifiedNeighbor(current);
			for (Node n : neighbor) {
				if (!visited.contains(n)) {
					visited.add(n);
					if (n.multiParents ==null){
						n.multiParents= new ArrayList<Node>();
					}
					n.multiParents.add(current);
					n.parent = current;
					nodeQueue.offer(n);
				}

			}

		}
		if (current == null) {
			return null;
		} else {
			Queue<Action> actions = new LinkedList<Action>();
			// the way from ——》 dest
			state.tmp_direction = state.agent_direction;
			while (current.parent != null) {
				Queue<Action> alist = shortestActionsForNeighbor(current, current.parent);
				while (alist.peek() != null) {
					actions.offer(alist.poll());
				}
				current = current.parent;

			}
			return actions;
		}
	}

	private List<Node> getQualifiedNeighbor(Node n) {
		List<Node> neighbor = new ArrayList<Node>();
		if (n.x_position + 1 < state.world.length
				&& state.world[n.x_position + 1][n.y_position] != state.UNKNOWN
				&& state.world[n.x_position + 1][n.y_position] != state.WALL) {
			neighbor.add(new Node(n.x_position + 1, n.y_position));

		}
		if (n.x_position - 1 >= 0 
				&& state.world[n.x_position - 1][n.y_position] != state.UNKNOWN
				&& state.world[n.x_position - 1][n.y_position] != state.WALL) {
			neighbor.add(new Node(n.x_position - 1, n.y_position));

		}
		if (n.y_position - 1 >= 0 
				&& state.world[n.x_position][n.y_position - 1] != state.UNKNOWN
				&& state.world[n.x_position][n.y_position - 1] != state.WALL) {
			neighbor.add(new Node(n.x_position, n.y_position - 1));

		}
		if (n.y_position + 1 < state.world[0].length 
				&& state.world[n.x_position][n.y_position + 1] != state.UNKNOWN
				&& state.world[n.x_position][n.y_position + 1] != state.WALL) {
			neighbor.add(new Node(n.x_position, n.y_position + 1));

		}
		return neighbor;
	}

	// for neighbor nodes
	private Queue<Action> shortestActionsForNeighbor(Node from, Node dest) {
		int xdiff = dest.x_position - from.x_position;
		int ydiff = dest.y_position - from.y_position;
		Queue<Action> actions = new LinkedList<Action>();
		// should take one step
		// d is on the right side of f
		if (xdiff > 0 && ydiff == 0) {
			switch (state.tmp_direction) {
			case DsfAgentState.NORTH:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);

				break;
			case DsfAgentState.EAST:
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			case DsfAgentState.SOUTH:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_LEFT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);

				break;
			case DsfAgentState.WEST:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_LEFT);
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_LEFT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			}
			state.tmp_direction = DsfAgentState.EAST;// 改变临时方向
		}
		// d is downside of f
		if (xdiff == 0 && ydiff > 0) {
			switch (state.tmp_direction) {
			case DsfAgentState.NORTH:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);

				break;
			case DsfAgentState.EAST:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			case DsfAgentState.SOUTH:
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);

				break;
			case DsfAgentState.WEST:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_LEFT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			}
			state.tmp_direction = DsfAgentState.SOUTH;// 改变临时方向
		}
		// d is upper f
		if (xdiff == 0 && ydiff < 0) {
			switch (state.tmp_direction) {
			case DsfAgentState.NORTH:
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			case DsfAgentState.EAST:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_LEFT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			case DsfAgentState.SOUTH:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			case DsfAgentState.WEST:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			}
			state.tmp_direction = DsfAgentState.NORTH;// 改变临时方向
		}

		// d on the left

		if (xdiff < 0 && ydiff == 0) {
			switch (state.tmp_direction) {
			case DsfAgentState.NORTH:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_LEFT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);

				break;
			case DsfAgentState.EAST:

				actions.offer(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			case DsfAgentState.SOUTH:
				actions.offer(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			case DsfAgentState.WEST:
				actions.offer(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				break;
			}
			state.tmp_direction = DsfAgentState.WEST;// 改变临时方向
		}

		return actions;

	}

}

public class RandomVacuumAgent extends AbstractAgent {
	public RandomVacuumAgent() {
		super(new DsfAgentProgram());
	}

}
