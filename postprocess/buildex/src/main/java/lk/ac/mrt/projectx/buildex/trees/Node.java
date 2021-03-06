package lk.ac.mrt.projectx.buildex.trees;

import javafx.util.Pair;
import lk.ac.mrt.projectx.buildex.models.output.Operand;
import lk.ac.mrt.projectx.buildex.x86.X86Analysis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static lk.ac.mrt.projectx.buildex.models.output.MemoryType.IMM_INT_TYPE;
import static lk.ac.mrt.projectx.buildex.models.output.MemoryType.MEM_HEAP_TYPE;
import static lk.ac.mrt.projectx.buildex.x86.X86Analysis.Operation.op_add;
import static lk.ac.mrt.projectx.buildex.x86.X86Analysis.Operation.op_mul;

/**
 * Created by krv on 12/4/2016.
 */
public abstract class Node implements Serializable {//chathura - generics removed

    final static Logger logger = LogManager.getLogger(Node.class);

    //TODO : Check access modifiers later
    //region Variables

    //region public variables

    public X86Analysis.Operation operation;  // Operation of this node
    public List<Node> srcs; ///< forward references also srcs of the destination
    public List<Node> prev; ///< keep the backward references
    public List<Integer> pos; ///< position of the parent node's srcs list for this child

    //endregion public variables

    //region unclassified variables

    public Boolean sign;   // Signed operation or not
    public Boolean minus;
    public String functionName;

    public Operand symbol;

    public Long pc;
    public Long line;

    // Auxiliary variables
    public Integer order_num;
    public Integer para_num;
    public Boolean is_para;
    public Boolean is_double;

    private Boolean visited;
    //endregion unclassified variables

    //endregion Variables

    //region public constructors

    public Node() {
        operation = null;
        sign = null;
        minus = null;
        functionName = null;
        srcs = new ArrayList<>();
        prev = new ArrayList<>();
        pos = new ArrayList<>();
        pc = null;
        line = pc;
        order_num = -1;
        para_num = null;
        is_double = null;
        is_para = null;
        visited = false;
    }

    public Node(Node node) {//todo is this for cloning? This won't actually deep copy. Migrate to clone method -chathura
        this.operation = node.operation;
        this.sign = node.sign;
        this.symbol = node.symbol;
        this.pc = node.pc;
        this.is_para = node.is_para;
        this.is_double = node.is_double;
        this.para_num = node.para_num;
        // no copying
        this.visited = false;
        this.order_num = -1;
    }

    //endregion public constructors

    //region public methods

    public static boolean isNodesSimilar(List<Node> nodes) {
        boolean ans = true;
        if (nodes.isEmpty()) {
            ans = true;
        } else {
            Node firstNode = nodes.get(0);
            for (Node node : nodes.subList(1, nodes.size() - 1)) {
                if (node != firstNode) {
                    ans = false;
                    break;
                }
            }
        }
        return ans;
    }

    public abstract String getNodeString();

    public abstract String getDotString();

    public abstract String getSimpleString();

    /**
     * Remove all forward references in source list
     */
    public void removeForwardReferenceAll() {
        for (int i = 0; i < srcs.size(); i++) {
            removeForwardReference(srcs.get(i));
        }
    }

    public int removeForwardReference(Node ref) {
        int count = 0;
        while (removeForwardReferenceSingle(ref)) {
            count++;
        }
        return count;
    }

    /**
     * This function is removing a one forward reference, forward references are in the srcs list
     * Because pos list contains backward references parents srcs lists positions for a node after every update of srcs
     * list need to update the pos list of all the forward references also
     *
     * @param ref
     * @return
     */
    private Boolean removeForwardReferenceSingle(Node ref) {
        // Removing from the src list
        int idx = srcs.indexOf(ref);
        if (srcs.remove(ref)) {
            // Updating the backward references of deleted node
            int jidx = ref.prev.indexOf(this);
            if (jidx != -1 && ref.pos.get(jidx) == idx) {
                ref.prev.remove(jidx);
                ref.pos.remove(jidx);
            } else {
                logger.warn("backward reference not found from deleted node");
            }
            // Update backward references of still connected nodes
            // Need to update pos list indexes of others connected to this
            //Since src list is changed
            // TODO : Check the possibility of being a own function
            for (int i = 0; i < srcs.size(); i++) {
                Node curSrcNode = srcs.get(i);
                int thisPosition = curSrcNode.prev.indexOf(this);
                if (thisPosition != -1) {
                    curSrcNode.pos.set(thisPosition, i);
                } else {
                    logger.warn("backward reference not found from src");
                }
                //TODO: Check why Chairth has used a difference logic
//                for (int j = 0; j < prev.size(); j++) {
//                    if (curSrcNode.prev.get(j) == this) {
//                        curSrcNode.pos.set(j, i);
//                        break;
//                    }
//                }
            }

            return true;
        }
        return false;
    }

    /**
     * This node has no backward refrences to others then delete backward references from others
     * to this and assume java delete this automatically
     * Note : Delete only if not the head
     *
     * @param head refrence to the head of the tree
     */
    public void safetly_delete(Node head) {
        if (prev.size() == 0 && this != head) {
            for (Node node : srcs) {
                node.removeBackwardReference(this);
            }
        }
    }

    public int removeBackwardReference(Node ref) {
        int count = 0;
        while (removeBackwardReferenceSingle(ref)) {
            count++;
        }
        return count;
    }

    private Boolean removeBackwardReferenceSingle(Node ref) {
        int idx = this.prev.indexOf(ref);
        if (idx != -1) {
            this.prev.remove(idx);
            this.pos.remove(idx);
            return true;
        }
        return true;
    }

    /**
     * Check whether this node refrence to any node having a indirect operation
     *
     * @return if found a node with indirect operation return the index, -1 otherwise
     */
    public int isIndirect() {
        for (int i = 0; i < srcs.size(); i++) {
            Node node = srcs.get(i);
            if (node.operation == X86Analysis.Operation.op_indirect) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Remove all srcs
     */
    public void removeAllForwardSrcs() {
        for (Node node : srcs) {
            this.removeForwardReference(node);
        }
    }

    /**
     * (dst -> this -> src)  => (dst -> src)
     *
     * @param dst
     * @param src
     */
    public void removeIntermediateNode(Node dst, Node src) {
        this.changeReference(dst, src);
        this.removeForwardReference(src);
    }

    /**
     * This operation will carry out the below logic at the end
     * (dst -> this) => (dst -> src)
     *
     * @param dst dst Node
     * @param src src Node
     */
    //TODO : Move to util class
    public void changeReference(Node dst, Node src) {
        /* In place forward reference and push back back ward reference
            replacing it at the exact same location is important for
	        non-associative operations*/
        //TODO : Check whether need to run a loop and do this to many srcs of dst, currently doing it only to idx index

        int idx = dst.srcs.indexOf(this);
        if (idx != -1) {
            dst.srcs.set(idx, src);
            src.prev.add(dst);
            src.pos.add(idx);
        }
//        dst.forwardReference(this); // Redundant
    }

    /**
     * This method remove the current node and lift its children up
     *
     * @return whether a congregation happened
     */
    public boolean congregateNode() {
        //TODO: logic is really bad (messing with loop variable)
        logger.debug("Entered Canonical node");
        boolean ret = false;
        for (int i = 0; i < this.prev.size(); i++) {
            if (this.operation.isOperationAssociative() && this.operation == this.prev.get(i).operation) {
                logger.debug("Canonical opportunity");
                Node pre_node = this.prev.get(i);
                int rem = pre_node.removeForwardReference(this);
                if (rem > 0) {
                    for (int j = 0; j < this.srcs.size(); j++) {
                        pre_node.addForwardReference(this.srcs.get(j));
                    }
                    i--;
                    ret = true;
                }
            }
        }
        return ret;
    }

    /**
     * Adding a node to srcs (forward) list
     *
     * @param ref node to be added
     */
    public void addForwardReference(Node ref) {
        this.srcs.add(ref);
        ref.prev.add(this);
        ref.pos.add(this.srcs.size() - 1);
    }

    /**
     * Implemented using the integer as the key not the node
     */
    public void orderNode() {
        List<Pair<Integer, Node>> other = new ArrayList<>();
        List<Pair<Integer, Node>> heap = new ArrayList<>();
        List<Pair<Integer, Node>> imm = new ArrayList<>();
        if (this.operation != op_mul && this.operation != op_add) {
            return;
        }

        for (int i = 0; i < this.srcs.size(); i++) {
            Node srcNode = (Node) this.srcs.get(i);
            Pair<Integer, Node> pair = new Pair<>(i, srcNode);
            if (srcNode.symbol.getType() == MEM_HEAP_TYPE) {
                heap.add(pair);
            } else if (srcNode.symbol.getType() == IMM_INT_TYPE) {
                imm.add(pair);
            } else {
                other.add(pair);
            }
        }

        Collections.sort(heap, new Comparator<Pair<Integer, Node>>() {
            @Override
            public int compare(Pair<Integer, Node> o1, Pair<Integer, Node> o2) {
                if (o1.getValue().symbol.getValue() instanceof Integer) {
                    return Integer.compare((Integer) o1.getValue().symbol.getValue(), (Integer) o2.getValue().symbol.getValue());
                } else {
                    return Double.compare((Double) o1.getValue().symbol.getValue(), (Double) o2.getValue().symbol.getValue());
                }
            }
        });

        this.srcs.clear();
        for (int i = 0; i < imm.size(); i++) {
            Node immNode = imm.get(i).getValue();
            Integer immValue = imm.get(i).getKey();
            this.srcs.add(immNode);
            for (int j = 0; j < immNode.prev.size(); j++) {
                if (immNode.prev.get(j) == this && immNode.pos.get(j) == immValue) {
                    immNode.pos.set( j, immValue );
                }
            }
        }

        for (int i = 0; i < other.size(); i++) {
            Node othNode = other.get(i).getValue();
            Integer othValue = other.get(i).getKey();
            this.srcs.add(othNode);
            for (int j = 0; j < othNode.prev.size(); j++) {
                if (othNode.prev.get(j) == this && othNode.pos.get(j) == othValue) {
                    othNode.pos.set(j, othValue);
                }
            }
        }

        for (int i = 0; i < heap.size(); i++) {
            Node heapNode = heap.get(i).getValue();
            Integer heapValue = other.get(i).getKey();
            this.srcs.add(heapNode);
            for (int j = 0; j < heapNode.prev.size(); j++) {
                if (heapNode.prev.get(j) == this && heapNode.pos.get(j) == heapValue) {
                    heapNode.pos.set(j, heapValue);
                }
            }
        }
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(Boolean visitOrNot) {
        this.visited = visitOrNot;
    }

    /**
     * Set the visited status to true
     */
    public void setVisited() {
        this.visited = true;
    }

    public X86Analysis.Operation getOperation() {
        return operation;
    }

    public void setOperation(X86Analysis.Operation operation) {
        this.operation = operation;
    }

    public Boolean getIsPara() {
        return is_para;
    }

    public void setIsPara(Boolean is_para) {
        this.is_para = is_para;
    }

    public void setIsPara() {
        this.is_para = true;
    }

    public Integer getParaNum() {
        return para_num;
    }

    public void setParaNum(Integer para_num) {
        this.para_num = para_num;
    }

//endregion public methods

    //region private methods

    public Integer getOrderNum() {
        return order_num;
    }

    public void setOrderNum(Integer order_num) {
        this.order_num = order_num;
    }

    public Operand getSymbol() {
        return symbol;
    }

    public List<Node> getSrcs() {
        return srcs;
    }

    public List<Node> getPrev() {
        return prev;
    }

    public List<Integer> getPos() {
        return pos;
    }

    public Long getPc() {
        return pc;
    }

    public void setPc(Long pc) {
        this.pc = pc;
    }

    public void setLine(Long line) {
        this.line = line;
    }

    public void setFunctionName(String funcName) {
        this.functionName = funcName;
    }

    //endregion private methods
}
