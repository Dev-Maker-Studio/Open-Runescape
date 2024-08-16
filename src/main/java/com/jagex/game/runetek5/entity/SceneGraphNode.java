package com.jagex.game.runetek5.entity;

import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.math.Quaternion;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.movement.CoordinateSpace;

/**
jag::graphics::GraphNode::AddChild(jag::graphics::GraphNode*)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         .text  00000000002CCE60 000001E4 00000038 00000000 R . . . . T .
jag::graphics::GraphNode::CalculateBounds(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  00000000000E1E40 00000092 00000018 00000000 R . . . . T .
jag::graphics::GraphNode::Detach(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .text  0000000000166880 0000009C 00000008 00000000 R . . . . T .
jag::graphics::GraphNode::GetWorldTransformationMatrixAligned(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   .text  00000000000C7760 00000242 00000048 00000000 R . . . . T .
jag::graphics::GraphNode::GetWorldTranslation(void) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           .text  00000000000C4FB0 00000205 00000018 00000000 R . . . . . .
jag::graphics::GraphNode::GraphNode(int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      .text  00000000002CD050 00000348 00000008 00000000 R . . . . . .
jag::graphics::GraphNode::MarkTreeInvalid(jag::graphics::GraphNode*,uchar) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000BE8D0 00000295 000000B8 00000000 R . . . . . .
jag::graphics::GraphNode::MarkTreeInvalid(jag::graphics::GraphNode*,uchar) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  00000000000BECB0 00000083 00000028 00000000 R . . . . . .
jag::graphics::GraphNode::RemoveChild(jag::graphics::GraphNode*)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      .text  00000000000D1710 00000084                   R . . . . T .
jag::graphics::GraphNode::Reset(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 .text  00000000000CE290 0000042A 000000E8 00000000 R . . . . T .
jag::graphics::GraphNode::SetAABB(jag::math::AABB const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  00000000000D16C0 00000043                   R . . . . . .
jag::graphics::GraphNode::UnionAABBWithChildren(bool)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 .text  000000000016B130 000001A9 00000028 00000000 R . . . . T .
jag::graphics::GraphNode::VisibilityTest(eastl::vector<std::pair<jag::graphics::GraphEntity *,jag::math::Matrix44 const*>,eastl::allocator> &,uint,jag::math::Frustum const&,int,int,std::function<bool ()(std::array<jag::math::IntersectionType,9ul> &,jag::math::Frustum const&,int,jag::math::AABB const&,jag::math::OBB const&)> const&,jag::math::AABB const*,jag::math::Frustum const*)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  000000000030B320 00000718 000000B8 00000018 R . . . . T .
jag::graphics::GraphNode::VisibilityTest(eastl::vector<std::pair<jag::graphics::GraphEntity *,jag::math::Matrix44 const*>,eastl::allocator> &,uint,jag::math::Frustum const&,int,int,std::function<bool ()(std::array<jag::math::IntersectionType,9ul> &,jag::math::Frustum const&,int,jag::math::AABB const&,jag::math::OBB const&)> const&,jag::math::AABB const*,jag::math::Frustum const*) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .text  0000000000336780 00000815 000000B8 00000018 R . . . . . .
jag::graphics::GraphNode::VisibilityTest(uint,jag::math::Frustum const&,int,int,jag::math::AABB const*,jag::math::Frustum const*) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  0000000000156030 000003DD 00000068 00000000 R . . . . . .
jag::graphics::GraphNode::VisibilityTrue(uint,int,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  00000000000D0710 0000012A 00000068 00000000 R . . . . . .
jag::graphics::GraphNode::~GraphNode() [clone]  
 */
class SceneGraphNode {

    public boolean calculateAbsolutePosition = true;
    public boolean aBool5945 = true;
    public boolean projectionUpdate;
    public SceneGraphNode aGraphNode5944;
    public SceneGraphNode aGraphNode5943;
    public SceneGraphNode aGraphNode5940;
    public final CoordinateSpace basePosition = new CoordinateSpace(); // offset
    
    /**
     * Absolute position (upscaled by << 2)
     */
    public final CoordinateSpace absolutePosition = new CoordinateSpace();
    public final CoordinateSpace aClass3055939 = new CoordinateSpace();
    public final Matrix43 projectionMatrix;

    public SceneGraphNode() {
        new Matrix44();
        new Matrix44();
        this.projectionMatrix = new Matrix43();
        this.projectionUpdate = true;
        this.aGraphNode5943 = null;
        this.aGraphNode5944 = null;
        this.aGraphNode5940 = null;
    }

    public CoordinateSpace getBasePosition() {
        return this.basePosition;
    }

    /**
     * Gets the absolute position (upscaled by << 2)
     */
    public CoordinateSpace getAbsolutePosition() {
        if (this.calculateAbsolutePosition) {
            this.calculateAbsolutePosition = false;
            if (this.aGraphNode5943 != null) {
                this.absolutePosition.copy(this.basePosition);
                this.absolutePosition.offset(this.aGraphNode5943.getAbsolutePosition());
            } else {
                this.absolutePosition.copy(this.basePosition);
            }
        }
        return this.absolutePosition;
    }

    public CoordinateSpace getOffset() {
        if (this.aBool5945) {
            this.aBool5945 = false;
            this.aClass3055939.copy(getAbsolutePosition());
            this.aClass3055939.negate();
        }

        return this.absolutePosition;
    }

    public  Matrix43 getProjectionMatrix() {
        if (this.projectionUpdate) {
            this.projectionUpdate = false;
            this.projectionMatrix.fromCoordinateSpace(getAbsolutePosition());
        }
        return this.projectionMatrix;
    }

    public void setBasePosition(Vector3 copyFrom) {
        this.basePosition.coords.copy(copyFrom);
        requestProjectionUpdate();
        if (this.aGraphNode5944 != null) {
            this.aGraphNode5944.update();
        }
    }

    public void setBasePosition(float baseX, float plane, float baseY) {
        this.basePosition.coords.set(baseX, plane, baseY);
        requestProjectionUpdate();
        if (this.aGraphNode5944 != null) {
            this.aGraphNode5944.update();
        }
    }
    
    public void setAbsolutePosition(CoordinateSpace abs) {
        if (this.aGraphNode5943 != null) {
            CoordinateSpace copy = new CoordinateSpace(abs);
            copy.offset(this.aGraphNode5943.getOffset());
            setBasePosition(copy);
        } else {
            setBasePosition(abs);
        }
    }
    
    public void setBasePosition(CoordinateSpace copyFrom) {
        this.basePosition.copy(copyFrom);
        requestProjectionUpdate();
        if (this.aGraphNode5944 != null) {
            this.aGraphNode5944.update();
        }
    }
    
    public void requestProjectionUpdate() {
        this.calculateAbsolutePosition = true;
        this.aBool5945 = true;
        this.projectionUpdate = true;
    }

    public void method11176() {
        if (this.aGraphNode5943 != null) {
        	SceneGraphNode sceneGraphNode = this.aGraphNode5943.aGraphNode5944;
            if (sceneGraphNode == this) {
                this.aGraphNode5943.aGraphNode5944 = this.aGraphNode5940;
            } else {
                while (this != sceneGraphNode.aGraphNode5940) {
                    sceneGraphNode = sceneGraphNode.aGraphNode5940;
                }

                sceneGraphNode.aGraphNode5940 = this.aGraphNode5940;
            }
        }

        requestProjectionUpdate();
        if (this.aGraphNode5944 != null) {
            this.aGraphNode5944.update();
            SceneGraphNode sceneGraphNode = this.aGraphNode5944;

            while (true) {
                sceneGraphNode.basePosition.offset(this.basePosition);
                sceneGraphNode.aGraphNode5943 = this.aGraphNode5943;
                if (sceneGraphNode.aGraphNode5940 == null) {
                    sceneGraphNode.aGraphNode5940 = this.aGraphNode5943.aGraphNode5944;
                    this.aGraphNode5943.aGraphNode5944 = this.aGraphNode5944;
                    break;
                }

                sceneGraphNode = sceneGraphNode.aGraphNode5940;
            }
        }

        this.aGraphNode5943 = null;
        this.aGraphNode5940 = null;
        this.aGraphNode5944 = null;
    }

    public void rotationUpdate(Quaternion quaternion) {
        this.basePosition.quaternion.copy(quaternion);
        requestProjectionUpdate();
        if (this.aGraphNode5944 != null) {
            this.aGraphNode5944.update();
        }

    }

    public void update() {
        requestProjectionUpdate();
        if (this.aGraphNode5944 != null) {
            this.aGraphNode5944.update();
        }

        if (this.aGraphNode5940 != null) {
            this.aGraphNode5940.update();
        }

    }
}
