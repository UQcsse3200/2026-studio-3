package com.csse3200.game.maps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Generates a seeded pool of typed map nodes without creating graph connections. */
public final class NodePoolGenerator {

  private NodePoolGenerator() {}

  /**
   * Generates the configured normal nodes followed by exactly one final node.
   *
   * @param config room distribution configuration
   * @return immutable node pool keyed by unique, sequential IDs
   * @throws NullPointerException if the configuration is null
   */
  public static Map<Integer, MapNode> generate(RoomDistributionConfig config) {
    if (config == null) {
      throw new NullPointerException("Config cannot be null!");
    }

    int nodeCount = config.getNormalNodeCount();
    List<RoomType> roomTypes = createRoomTypes(config);
    Long seed = config.getSeed();
    Random random = seed == null ? new Random() : new Random(seed);

    Collections.shuffle(roomTypes, random); // Shuffleeeee
    Map<Integer, MapNode> nodes = new HashMap<>(nodeCount + 1);

    // Using zero-based IDs here: 0 to nodeCount - 1.
    for (int index = 0; index < roomTypes.size(); index++) {
      nodes.put(index, new MapNode(index, roomTypes.get(index)));
    }

    nodes.put(nodeCount, new MapNode(nodeCount, RoomType.FINAL));
    return Map.copyOf(nodes);
  }

  /** Calculates proportional room counts and creates the room-type list. */
  private static List<RoomType> createRoomTypes(RoomDistributionConfig config) {
    RoomType[] types = {RoomType.COMBAT, RoomType.EVENT, RoomType.SHOP};
    int[] weights = {config.getCombatWeight(), config.getEventWeight(), config.getShopWeight()};

    int nodeCount = config.getNormalNodeCount();
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

    List<RoomType> roomTypes = new ArrayList<>(nodeCount);

    // Chuck the room types node into the list :D
    for (int index = 0; index < types.length; index++) {
      for (int count = 0; count < counts[index]; count++) {
        roomTypes.add(types[index]);
      }
    }

    return roomTypes;
  }
}
