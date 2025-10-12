package com.jivs.platform.service.businessobject;

import com.jivs.platform.domain.businessobject.BusinessObject;
import com.jivs.platform.repository.BusinessObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing business object hierarchies
 */
@Service
@RequiredArgsConstructor
public class HierarchyService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HierarchyService.class);

    private final BusinessObjectRepository businessObjectRepository;

    /**
     * Build complete hierarchy from root
     */
    public BusinessObjectHierarchy buildHierarchy(BusinessObject root) {
        BusinessObjectHierarchy hierarchy = new BusinessObjectHierarchy();
        hierarchy.setRoot(root);

        // Build tree structure
        HierarchyNode rootNode = buildHierarchyNode(root);
        hierarchy.setRootNode(rootNode);

        // Calculate statistics
        hierarchy.setTotalNodes(countNodes(rootNode));
        hierarchy.setMaxDepth(calculateMaxDepth(rootNode, 0));
        hierarchy.setLeafCount(countLeafNodes(rootNode));

        return hierarchy;
    }

    /**
     * Build hierarchy node recursively
     */
    private HierarchyNode buildHierarchyNode(BusinessObject businessObject) {
        HierarchyNode node = new HierarchyNode();
        node.setBusinessObject(businessObject);

        List<BusinessObject> children = businessObjectRepository.findByParent(businessObject);
        if (!children.isEmpty()) {
            List<HierarchyNode> childNodes = children.stream()
                .map(this::buildHierarchyNode)
                .collect(Collectors.toList());
            node.setChildren(childNodes);
        }

        return node;
    }

    /**
     * Get path from root to node
     */
    public List<BusinessObject> getPath(BusinessObject businessObject) {
        List<BusinessObject> path = new ArrayList<>();
        BusinessObject current = businessObject;

        while (current != null) {
            path.add(0, current); // Add at beginning to maintain order
            current = current.getParent();
        }

        return path;
    }

    /**
     * Get all descendants of a business object
     */
    public List<BusinessObject> getAllDescendants(BusinessObject businessObject) {
        List<BusinessObject> descendants = new ArrayList<>();
        collectDescendants(businessObject, descendants);
        return descendants;
    }

    /**
     * Recursively collect descendants
     */
    private void collectDescendants(BusinessObject parent, List<BusinessObject> descendants) {
        List<BusinessObject> children = businessObjectRepository.findByParent(parent);
        descendants.addAll(children);

        for (BusinessObject child : children) {
            collectDescendants(child, descendants);
        }
    }

    /**
     * Move business object to new parent
     */
    public void moveToParent(BusinessObject businessObject, BusinessObject newParent) {
        // Validate move
        if (newParent != null && wouldCreateCycle(businessObject, newParent)) {
            throw new IllegalStateException("Cannot move - would create cycle in hierarchy");
        }

        // Update parent
        businessObject.setParent(newParent);

        // Update level
        int newLevel = newParent != null ? newParent.getLevel() + 1 : 0;
        updateLevels(businessObject, newLevel);

        businessObjectRepository.save(businessObject);
    }

    /**
     * Update levels recursively
     */
    private void updateLevels(BusinessObject businessObject, int newLevel) {
        businessObject.setLevel(newLevel);

        List<BusinessObject> children = businessObjectRepository.findByParent(businessObject);
        for (BusinessObject child : children) {
            updateLevels(child, newLevel + 1);
        }
    }

    /**
     * Check if move would create a cycle
     */
    private boolean wouldCreateCycle(BusinessObject businessObject, BusinessObject newParent) {
        BusinessObject current = newParent;
        while (current != null) {
            if (current.getId().equals(businessObject.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Get siblings of a business object
     */
    public List<BusinessObject> getSiblings(BusinessObject businessObject) {
        if (businessObject.getParent() == null) {
            return businessObjectRepository.findByParentIsNull().stream()
                .filter(obj -> !obj.getId().equals(businessObject.getId()))
                .collect(Collectors.toList());
        }

        return businessObjectRepository.findByParent(businessObject.getParent()).stream()
            .filter(obj -> !obj.getId().equals(businessObject.getId()))
            .collect(Collectors.toList());
    }

    /**
     * Count total nodes in hierarchy
     */
    private int countNodes(HierarchyNode node) {
        int count = 1; // Current node

        if (node.getChildren() != null) {
            for (HierarchyNode child : node.getChildren()) {
                count += countNodes(child);
            }
        }

        return count;
    }

    /**
     * Calculate maximum depth
     */
    private int calculateMaxDepth(HierarchyNode node, int currentDepth) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            return currentDepth;
        }

        int maxDepth = currentDepth;
        for (HierarchyNode child : node.getChildren()) {
            int childDepth = calculateMaxDepth(child, currentDepth + 1);
            maxDepth = Math.max(maxDepth, childDepth);
        }

        return maxDepth;
    }

    /**
     * Count leaf nodes
     */
    private int countLeafNodes(HierarchyNode node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            return 1;
        }

        int count = 0;
        for (HierarchyNode child : node.getChildren()) {
            count += countLeafNodes(child);
        }

        return count;
    }
}

/**
 * Business object hierarchy model
 */
class BusinessObjectHierarchy {
    private BusinessObject root;
    private HierarchyNode rootNode;
    private int totalNodes;
    private int maxDepth;
    private int leafCount;

    // Getters and setters
    public BusinessObject getRoot() { return root; }
    public void setRoot(BusinessObject root) { this.root = root; }
    public HierarchyNode getRootNode() { return rootNode; }
    public void setRootNode(HierarchyNode rootNode) { this.rootNode = rootNode; }
    public int getTotalNodes() { return totalNodes; }
    public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
    public int getLeafCount() { return leafCount; }
    public void setLeafCount(int leafCount) { this.leafCount = leafCount; }
}

/**
 * Hierarchy node model
 */
class HierarchyNode {
    private BusinessObject businessObject;
    private List<HierarchyNode> children;

    // Getters and setters
    public BusinessObject getBusinessObject() { return businessObject; }
    public void setBusinessObject(BusinessObject businessObject) {
        this.businessObject = businessObject;
    }
    public List<HierarchyNode> getChildren() { return children; }
    public void setChildren(List<HierarchyNode> children) { this.children = children; }
}
