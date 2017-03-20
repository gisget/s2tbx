package org.esa.s2tbx.grm;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.esa.s2tbx.grm.tiles.ProcessingTile;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jean Coravu
 */
public class Graph {
    private final ArrayListExtended<Node> nodes;

    public Graph(int numberOfNodes) {
        this.nodes = new ArrayListExtended<Node>(numberOfNodes);
    }

    public Node getNodeAt(int index) {
        return this.nodes.get(index);
    }

    public void addNode(Node nodeToAdd) {
        this.nodes.add(nodeToAdd);
    }

    public int getNodeCount() {
        return this.nodes.size();
    }

    public int removeExpiredNodes() {
        int nodeCount = this.nodes.size();
        int lastIndexToCopy = -1;
        for (int i=0; i<nodeCount; i++) {
            Node node = this.nodes.get(i);
            if (node.isExpired()) {
                if (lastIndexToCopy == -1) {
                    lastIndexToCopy = i;
                }
            } else if (lastIndexToCopy > -1) {
                this.nodes.set(lastIndexToCopy, node);
                lastIndexToCopy++;
            }
        }
        if (lastIndexToCopy > - 1 && lastIndexToCopy < nodeCount) {
            this.nodes.removeItems(lastIndexToCopy, nodeCount);
        }
        return this.nodes.size();
    }

    public void setValidFlagToAllNodes() {
        int nodeCount = this.nodes.size();
        for (int i=0; i<nodeCount; i++) {
            Node n = this.nodes.get(i);
            n.setValid(true);
        }
    }

    public void resetMergedFlagToAllNodes() {
        int nodeCount = this.nodes.size();
        for (int i=0; i<nodeCount; i++) {
            Node n = this.nodes.get(i);
            n.setMerged(false);
        }
    }

    public void resetCostUpdatedFlagToAllEdges() {
        int nodeCount = this.nodes.size();
        for (int i=0; i<nodeCount; i++) {
            Node n = this.nodes.get(i);
            int edgeCount = n.getEdgeCount();
            for (int j=0; j<edgeCount; j++) {
                Edge edge = n.getEdgeAt(j);
                edge.setCostUpdated(false);
            }
        }
    }

    public Object2IntMap<Node> detectBorderNodes(ProcessingTile tile, int imageWidth, int imageHeight) {
        Object2IntMap<Node> borderNodesMap = new Object2IntArrayMap<Node>();
        int nodeCount = this.nodes.size();
        for (int i=0; i<nodeCount; i++) {
            Node node = this.nodes.get(i);
            BoundingBox box = node.getBox();
            if (box.getLeftX() > tile.getImageLeftX() && box.getTopY() > tile.getImageTopY() && box.getRightX() - 1 < tile.getImageRightX() && box.getBottomY() - 1 < tile.getImageBottomY()) {
                // the node is inside the tile
                continue;
            } else {
                // the node is on the tile margin or outside the tile
                IntSet borderCells = AbstractSegmenter.generateBorderCells(node.getContour(), node.getId(), imageWidth);
                IntIterator it = borderCells.iterator();
                while (it.hasNext()) {
                    int gridIdInImage = it.nextInt();
                    int rowPixelInImage = gridIdInImage / imageWidth;
                    int colPixelInImage = gridIdInImage % imageWidth;
                    if (tile.getImageTopY() > 0 && rowPixelInImage == tile.getImageTopY()) {
                        borderNodesMap.put(node, 0);
                        break;
                    } else if (tile.getImageRightX() < imageWidth - 1 && colPixelInImage == tile.getImageRightX()) {
                        borderNodesMap.put(node, 0);
                        break;
                    } else if (tile.getImageBottomY() < imageHeight - 1 && rowPixelInImage == tile.getImageBottomY()) {
                        borderNodesMap.put(node, 0);
                        break;
                    } else if (tile.getImageLeftX() > 0 && colPixelInImage == tile.getImageLeftX()) {
                        borderNodesMap.put(node, 0);
                        break;
                    }
                }
            }
        }

        return borderNodesMap;
    }

    public Int2ObjectMap<List<Node>> buildBorderPixelMap(ProcessingTile tile, int rowTileIndex, int colTileIndex, int nbTilesX, int nbTilesY, int imageWidth) {
        Int2ObjectMap<List<Node>> borderPixelMap = new Int2ObjectArrayMap<List<Node>>(); // key = node id

        int rowMin = (tile.getImageTopY() > 0) ? tile.getImageTopY() - 1 : tile.getImageTopY();
        int rowMax = tile.getImageBottomY() + 1;
        int colMin = (tile.getImageLeftX() > 0) ? tile.getImageLeftX() - 1 : tile.getImageLeftX();
        int colMax = tile.getImageRightX() + 1;

        int nodeCount = this.nodes.size();
        for (int i=0; i<nodeCount; i++) {
            Node node = this.nodes.get(i);
            BoundingBox box = node.getBox();
            if (box.getLeftX() > tile.getImageLeftX() && box.getTopY() > tile.getImageTopY() && box.getRightX() - 1 < tile.getImageRightX() && box.getBottomY() - 1 < tile.getImageBottomY()) {
                continue;
            } else {
                IntSet borderCells = AbstractSegmenter.generateBorderCells(node.getContour(), node.getId(), imageWidth);
                IntIterator it = borderCells.iterator();
                while (it.hasNext()) {
                    int gridId = it.nextInt();
                    int rowPixel = gridId / imageWidth;
                    int colPixel = gridId % imageWidth;
                    boolean addNode = false;
                    if (rowTileIndex > 0 && (rowPixel == tile.rows[0] || rowPixel == rowMin)) {
                        addNode = true;
                    } else if (colTileIndex < nbTilesX - 1 && (colPixel == tile.columns[1] || colPixel == colMax)) {
                        addNode = true;
                    } else if (rowTileIndex < nbTilesY - 1 && (rowPixel == tile.rows[1] || rowPixel == rowMax)) {
                        addNode = true;
                    } else if (colTileIndex > 0 && (colPixel == tile.columns[0] || colPixel == colMin)) {
                        addNode = true;
                    }
                    if (addNode) {
                        List<Node> nodes = borderPixelMap.get(gridId);
                        if (nodes == null) {
                            nodes = new ArrayList<Node>();
                            borderPixelMap.put(gridId, nodes);
                        }
                        nodes.add(node);
                    }
                }
            }
        }
        return borderPixelMap;
    }

    public void removeDuplicatedNodes(Int2ObjectMap<List<Node>> borderPixelMap, int imageWidth) {
        ObjectIterator<Int2ObjectMap.Entry<List<Node>>> it = borderPixelMap.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            Int2ObjectMap.Entry<List<Node>> entry = it.next();
            List<Node> nodes = entry.getValue();
            if (nodes.size() > 1) {
                Node refNode = nodes.get(0); // refNode
                // explore duplicated nodes
                for (int i=1; i<nodes.size(); i++) {
                    Node currentNode = nodes.get(i);
                    int edgeCount = currentNode.getEdgeCount();
                    for (int k=0; k<edgeCount; k++) {
                        Edge edge = currentNode.getEdgeAt(k);
                        Node neighNit = edge.getTarget();
                        int removedEdgeIndex = neighNit.removeEdge(currentNode);
                        assert(removedEdgeIndex >= 0);

                        Edge edgeToFirstNode = neighNit.findEdge(refNode);
                        if (edgeToFirstNode == null) {
                            // create an edge neighNit -> refNode
                            neighNit.addEdge(refNode, edge.getBoundary());
                            // create an edge refNode -> neighNit
                            refNode.addEdge(neighNit, edge.getBoundary());
                        }
                    }
                    currentNode.setExpired(true);
                }

                IntSet borderCells = AbstractSegmenter.generateBorderCells(refNode.getContour(), refNode.getId(), imageWidth);
                IntIterator itCells = borderCells.iterator();
                while (itCells.hasNext()) {
                    int gridId = itCells.nextInt();
                    List<Node> resultNodes = borderPixelMap.get(gridId);
                    if (resultNodes != null) {
                        resultNodes.clear();
                        resultNodes.add(refNode);
                    }
                }
            }
        }
        removeExpiredNodes();
    }

    public void removeUnstableSegments(ProcessingTile tile, int imageWidth) {
        int nodeCount = this.nodes.size();
        for (int i=0; i<nodeCount; i++) {
            Node node = this.nodes.get(i);
            BoundingBox box = node.getBox();
            if (box.getLeftX() >= tile.getImageLeftX() && box.getTopY() >= tile.getImageTopY() && box.getRightX() - 1 <= tile.getImageRightX() && box.getBottomY() - 1 <= tile.getImageBottomY()) {
                continue;
            } else if (box.getLeftX() > tile.getImageRightX() || box.getTopY() > tile.getImageBottomY() || box.getRightX() - 1 < tile.getImageLeftX()
                       || box.getBottomY() - 1 < tile.getImageTopY()) {
                node.setExpired(true);
                removeEdgeToUnstableNode(node);
            } else {
                IntSet borderCells = AbstractSegmenter.generateBorderCells(node.getContour(), node.getId(), imageWidth);
                boolean stable = false;
                IntIterator it = borderCells.iterator();
                while (it.hasNext()) {
                    int gridIdInImage = it.nextInt();
                    int rowPixelInImage = gridIdInImage / imageWidth;
                    int colPixelInImage = gridIdInImage % imageWidth;
                    if (rowPixelInImage >= tile.getImageTopY() && rowPixelInImage <= tile.getImageBottomY() && colPixelInImage >= tile.getImageLeftX() && colPixelInImage <= tile.getImageRightX()) {
                        stable = true;
                        break;
                    }
                }
                if (!stable) {
                    node.setExpired(true);
                    removeEdgeToUnstableNode(node);
                }
            }
        }
        removeExpiredNodes();
    }

    public void removeEdgeToUnstableNode(Node node) {
        int edgeCount = node.getEdgeCount();
        for (int j=0; j<edgeCount; j++) {
            Edge edge = node.getEdgeAt(j);
            Node nodeNeighbor = edge.getTarget();
            int removedEdgeIndex = nodeNeighbor.removeEdge(node);
            assert(removedEdgeIndex >= 0);
        }
    }

    public void rescaleGraph(ProcessingTile tile, int rowTileIndex, int colTileIndex, int tileWidth, int tileHeight, int imageWidth) {
        int rowNodeTile = 0;
        int colNodeTile = 0;
        int rowNodeImg = 0;
        int colNodeImg = 0;
        int regionWidth = tile.getRegion().getWidth();
        int nodeCount = this.nodes.size();
//        System.out.println("rescaleGraph nodeCount="+nodeCount+" rowTileIndex="+rowTileIndex+" colTileIndex="+colTileIndex+" tileWidth="+tileWidth+" tileHeight="+tileHeight);

        for (int i=0; i<nodeCount; i++) {
            Node node = this.nodes.get(i);

            // start pixel index of the node (in the tile)
            rowNodeTile = node.getId() / regionWidth;
            colNodeTile = node.getId() % regionWidth;

            // start pixel index of the node (in the image)
            rowNodeImg = rowTileIndex * tileHeight + rowNodeTile - tile.getTopMargin();
            colNodeImg = colTileIndex * tileWidth + colNodeTile - tile.getLeftMargin();

//            System.out.print("node: "+rowNodeTile+" "+colNodeTile+" "+rowNodeImg+" "+colNodeImg+" old id="+node.getId());

            // set the node id in the image
            node.setId(rowNodeImg * imageWidth + colNodeImg);

//            System.out.println(" new id="+node.getId());

            // change also its bounding box
            BoundingBox box = node.getBox();
            box.setLeftX(colTileIndex * tileWidth + box.getLeftX() - tile.getLeftMargin());
            box.setTopY(rowTileIndex * tileHeight + box.getTopY() - tile.getTopMargin());
        }
    }

    private static class ArrayListExtended<ItemType> extends ArrayList<ItemType> {

        ArrayListExtended(int initialCapacity) {
            super(initialCapacity);
        }

        void removeItems(int fromIndex, int toIndex) {
            removeRange(fromIndex, toIndex);
        }
    }
}
