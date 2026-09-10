package com.csse3200.game.maps;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates a seeded pool of typed map nodes without creating graph
 * connections.
 */
public final class NodePoolGenerator {

  private NodePoolGenerator() {
  }

  /**
   * Generates placeholder combat nodes followed by exactly one final node and one
   * start node.
   *
   * @param config room distribution configuration
   * @return immutable node pool keyed by unique, sequential IDs
   * @throws NullPointerException if the configuration is null
   */
  public static Map<Integer, MapNode> generate(MapGenerationConfig config) {
    if (config == null) {
      throw new NullPointerException("Config cannot be null!");
    }

    int nodeCount = config.getNormalNodeCount();
    Map<Integer, MapNode> nodes = new HashMap<>(nodeCount + 1);

    // Using zero-based IDs here: 0 to nodeCount - 1.
    for (int index = 1; index < config.getNormalNodeCount(); index++) {
      nodes.put(index, new MapNode(index, RoomType.COMBAT));
    }
    nodes.put(0, new MapNode(0, RoomType.START));

    nodes.put(nodeCount, new MapNode(nodeCount, RoomType.FINAL));
    return Map.copyOf(nodes);
  }

  /**
   * Assigns node types to a map after path generation is completed to ensure
   * accurate weights.
   * 
   * @param config room distribution configuration
   * @param rand   random type passed from superclass
   * @param map    mapgraph to act on
   */
  public static void rebalanceRoomTypes(MapGenerationConfig config, Random rand, MapGraph map) {

    RoomType[] types = { RoomType.COMBAT, RoomType.EVENT, RoomType.SHOP };
    int[] counts = getRoomTypeCounts(config, map.getNodes().size());

    for (int i = 0; i < types.length; i++) {
      while (map.getNodesByType(types[i]).size() < counts[i]) {

        MapNode node = map.getNode(rand.nextInt(1, MapGenerationConfig.MAX_NODE_COUNT - 1));
        if (node != null) {
          assignRoomType(node, types[i]);
        }
      }
    }
  }

  /**
   * Allows constraints to be placed on how room types are placed.
   * For example, restricting shops from existing below layer 3.
   * 
   * @param config room distribution configuration
   * @param rand   random type passed from superclass
   * @param map    mapgraph to act on
   */
  private static void assignRoomType(MapNode node, RoomType room) {
    switch (room) {
      case COMBAT:
        node.setRoomType(room);
        break;
      case EVENT, SHOP:
        if (node.getHeight() > 2) {
          node.setRoomType(room);
        }
        break;
        default:
        break;
    }
  }

  /**
   * Performs a fixed point arithmetic calculation to determine the proportion of
   * room types required for given size.
   *
   * @param config    map generation config
   * @param nodeCount number of nodes to get proportion of
   * @return integer array of the required room counts from proportions
   */
  private static int[] getRoomTypeCounts(MapGenerationConfig config, int nodeCount) {
    RoomType[] types = { RoomType.COMBAT, RoomType.EVENT, RoomType.SHOP };
    int[] weights = { config.getCombatWeight(), config.getEventWeight(), config.getShopWeight() };

    long totalWeight = config.getTotalWeight();
    int[] counts = new int[types.length];
    long[] remainders = new long[types.length];
    int allocated = 0;

    for (int index = 0; index < types.length; index++) {

      long weightedCount = (long) nodeCount * weights[index];

      counts[index] = (int) (weightedCount / totalWeight);
      remainders[index] = weightedCount % totalWeight;
      allocated += counts[index];
    }

    // Give Remaining Nodeto room types with the largest remainders.
    while (allocated < nodeCount) {
      int largestRemainderIndex = 0;

      for (int index = 1; index < remainders.length; index++) {
        if (remainders[index] > remainders[largestRemainderIndex]) {
          largestRemainderIndex = index;
        }
      }

      counts[largestRemainderIndex]++;
      remainders[largestRemainderIndex] = -1;
      allocated++;
    }

    return counts;
  }
}
