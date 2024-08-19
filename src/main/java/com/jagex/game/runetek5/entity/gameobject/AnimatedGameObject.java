package com.jagex.game.runetek5.entity.gameobject;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeResult;
import com.jagex.game.runetek5.config.object.ObjectTypeCustomisation;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.config.sequence.EntitySequence;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystem;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.vardomain.VarProviders;

public class AnimatedGameObject {

    public boolean hasSequenceId;

    public ObjectTypeCustomisation locCustomisation;

    public Model pairModel;

    public boolean[] castShadowAtPlane;

    public Shadow shadow;

    public ParticleSystem particleSystem;

    public int currentCycle;

    public boolean aBool1552;

    public final boolean overlayHidden;

    public int gameobjectId = -1;

    public int modelMinY;

    public final ObjectTypeList loctypelist;

    public final int locId;

    public final int type;

    public final int rotation;

    public final SceneGraphEntity entityNode;

    public final byte logicHeight;

    public final byte heightLevel;

    public final boolean renderShadow;

    public final Sequence sequence;

    public AnimatedGameObject(AbstractToolkit renderer, ObjectTypeList loctypelist, ObjectType loctype, int type, int rotation,
                              int logicHeight, int plane, SceneGraphEntity transform, boolean overlayHidden, int sequenceId) {
        this.loctypelist = loctypelist;
        this.locId = loctype.id;
        this.type = type;
        this.rotation = rotation;
        this.entityNode = transform;
        this.hasSequenceId = sequenceId != -1;
        this.logicHeight = (byte) logicHeight;
        this.heightLevel = (byte) plane;
        this.overlayHidden = overlayHidden;
        this.renderShadow = renderer.hasShadows() && loctype.dynamicShadow && !this.overlayHidden;
        this.sequence = new EntitySequence(transform);
        this.animate(false, sequenceId, 1);
    }

    public void animate(int sequenceId) {
        this.hasSequenceId = true;
        animate(false, sequenceId, 1);
    }

    public int getMinY() {
        return this.modelMinY;
    }

    public void addCustomisation(ObjectTypeCustomisation customisation) {
        this.locCustomisation = customisation;
        this.pairModel = null;
    }

    public boolean hasRenderingShadow() {
        return this.renderShadow;
    }

    public void removeShadow() {
        if (this.shadow != null) {
            Vector3 abs = this.entityNode.getAbsolutePosition().coords;
            Resource.mapRegion.getScene().removeShadow(this.shadow, this.heightLevel, (int) abs.x, (int) abs.z, this.castShadowAtPlane);
            this.castShadowAtPlane = null;
            this.shadow = null;
        }
    }

    public void animateParticles(AbstractToolkit renderer, Model model, Matrix43 projectionMatrix, boolean performCycle) {
        EmissiveTriangle[] emitters = model.getEmissiveTriangles();
        EffectiveVertex[] effectiveVertices = model.getEffectiveVertices();
        if ((this.particleSystem == null || this.particleSystem.isDisabled) && (emitters != null || effectiveVertices != null)) {
            ObjectType locType = this.loctypelist.list(this.locId);
            if (locType.alternativeIds != null) {
                locType = locType.getMultiLoc(client.MAP_LOADING_STAGE == 4 ? VarProviders.CUTSCENE_ACTION_VAR_PROVIDER : VarProviders.PLAYER_VAR_PROVIDER);
            }
            if (locType != null) {
                this.particleSystem = ParticleSystem.get(client.cycles, true);
            }
        }
        
        if (this.particleSystem != null) {
            model.updateParticlePosition(projectionMatrix);
            if (performCycle) {
                this.particleSystem.performCycle(renderer, client.cycles, emitters, effectiveVertices);
            } else {
                this.particleSystem.setCurrentCycle(client.cycles);
            }
            this.particleSystem.setLogicHeight(this.logicHeight);
        }
    }

    public int getNegativeMinY() {
        return -getMinY();
    }

    public void animate(boolean isTransformed, int seqId, int animFlags) {
        int additionalAnimationFlag = animFlags;
        int randomSequenceId = seqId;
        boolean replay = false;
        if (seqId == -1) {
            ObjectType computedLoc = this.loctypelist.list(this.locId);
            ObjectType originalLoc = computedLoc;
            if (computedLoc.alternativeIds != null) {
                computedLoc = computedLoc.getMultiLoc(client.MAP_LOADING_STAGE == 4 ? VarProviders.CUTSCENE_ACTION_VAR_PROVIDER : VarProviders.PLAYER_VAR_PROVIDER);
            }
            if (computedLoc == null) {
                return;
            }
            if (originalLoc == computedLoc) {
                originalLoc = null;
            }
            if (computedLoc.isAnimated()) {
                if (isTransformed && this.sequence.hasType() && computedLoc.hasSequence(this.sequence.getId())) {
                    return;
                }
                if (computedLoc.id != this.gameobjectId) {
                    replay = computedLoc.replaySequence;
                }
                randomSequenceId = computedLoc.getRandomAnimation();
                if (computedLoc.hasAdditionalAnimations()) {
                    additionalAnimationFlag = 0;
                } else {
                    additionalAnimationFlag = 1;
                }
            } else if (originalLoc != null && originalLoc.isAnimated()) {
                if (isTransformed && this.sequence.hasType() && originalLoc.hasSequence(this.sequence.getId())) {
                    return;
                }
                if (this.gameobjectId != computedLoc.id) {
                    replay = originalLoc.replaySequence;
                }
                randomSequenceId = originalLoc.getRandomAnimation();
                if (originalLoc.hasAdditionalAnimations()) {
                    additionalAnimationFlag = 0;
                } else {
                    additionalAnimationFlag = 1;
                }
            }
        }
        if (randomSequenceId == -1) {
            this.sequence.resetSequence();
        } else {
            this.sequence.animateSequence(randomSequenceId, 0, additionalAnimationFlag, replay);
            this.currentCycle = client.cycles;
            this.aBool1552 = false;
            this.pairModel = null;
        }
    }

    public Model getAnimatedModel(AbstractToolkit renderer, int functionMask, boolean bool3, boolean hasShadow) {
        ObjectType loctype = this.loctypelist.list(this.locId);
        if (loctype.alternativeIds != null) {
            loctype = loctype.getMultiLoc(client.MAP_LOADING_STAGE == 4 ? VarProviders.CUTSCENE_ACTION_VAR_PROVIDER : VarProviders.PLAYER_VAR_PROVIDER);
        }
        if (loctype == null) {
            removeShadow();
            this.gameobjectId = -1;
            return null;
        } 
        if (!this.hasSequenceId && this.gameobjectId != loctype.id) {
            animate(true, -1, 0);
            this.aBool1552 = false;
            this.pairModel = null;
        }
        method2133();
        if (hasShadow) {
            hasShadow &= this.renderShadow & !this.aBool1552 & PreferenceManager.preferences.sceneryShadows.getValue() != 0;
        }
        if (bool3 && !hasShadow) {
            this.gameobjectId = loctype.id;
            return null;
        } 
        Vector3 scenePosition = this.entityNode.getAbsolutePosition().coords;
        Scene scene = Resource.mapRegion.getScene();
        if (hasShadow) {
            scene.removeShadow(this.shadow, this.heightLevel, (int) scenePosition.x, (int) scenePosition.z, this.castShadowAtPlane);
            this.aBool1552 = false;
        }
        Ground currentHeight = scene.localHeightmap[this.heightLevel];
        Ground nextHeight;
        if (this.overlayHidden) {
            nextHeight = scene.surfaceHeightmap[0];
        } else {
            nextHeight = this.heightLevel < 3 ? scene.localHeightmap[this.heightLevel + 1] : null;
        }
        Model model = null;
        if (this.sequence.hasType()) {
            if (hasShadow) {
                functionMask |= SHADOWED;
            }
            model = loctype.getAnimatedModel(renderer, functionMask, this.type != 11 ? this.type : 10, this.type == 11 ? 4 + this.rotation : this.rotation, currentHeight, nextHeight, (int) scenePosition.x, currentHeight.getAverageHeight((int) scenePosition.x, (int) scenePosition.z), (int) scenePosition.z, this.sequence, this.locCustomisation);
            if (model != null) {
                if (hasShadow) {
                    if (this.castShadowAtPlane == null) {
                        this.castShadowAtPlane = new boolean[4];
                    }
                    this.shadow = model.createShadowSprite(this.shadow);
                    scene.renderShadows(this.shadow, this.heightLevel, (int) scenePosition.x, (int) scenePosition.z, this.castShadowAtPlane);
                    this.aBool1552 = true;
                }
                this.modelMinY = model.getMinY();
                model.getSize3d();
            } else {
                this.castShadowAtPlane = null;
                this.shadow = null;
                this.modelMinY = 0;
            }
            this.pairModel = null;
        } else if (this.pairModel != null && (this.pairModel.getFunctionMask() & functionMask) == functionMask && this.gameobjectId == loctype.id) {
            model = this.pairModel;
        } else {
            if (this.pairModel != null) {
                functionMask |= this.pairModel.getFunctionMask();
            }
            TypeResult pair = loctype.getPair(renderer, functionMask, this.type != 11 ? this.type : 10, this.type == 11 ? 4 + this.rotation : this.rotation, currentHeight, nextHeight, (int) scenePosition.x, currentHeight.getAverageHeight((int) scenePosition.x, (int) scenePosition.z), (int) scenePosition.z, hasShadow, this.locCustomisation);
            if (pair != null) {
                this.pairModel = model = (Model) pair.mainObject;
                if (hasShadow) {
                    this.shadow = (Shadow) pair.companionObject;
                    this.castShadowAtPlane = null;
                    scene.renderShadows(this.shadow, this.heightLevel, (int) scenePosition.x, (int) scenePosition.z, null);
                    this.aBool1552 = true;
                }
                this.modelMinY = model.getMinY();
                model.getSize3d();
            } else {
                this.castShadowAtPlane = null;
                this.shadow = null;
                this.pairModel = null;
                this.modelMinY = 0;
            }
        }
        this.gameobjectId = loctype.id;
        return model;
    }

    public void method2133() {
        if (this.sequence.hasType()) {
            if (this.sequence.progressCycle(client.cycles - this.currentCycle)) {
                if (PreferenceManager.preferences.sceneryShadows.getValue() == 2) {
                    this.aBool1552 = false;
                }
                if (this.sequence.needsReset()) {
                    this.sequence.update(-1);
                    this.hasSequenceId = false;
                    animate(false, -1, 0);
                }
            }
        } else {
            animate(false, -1, 0);
        }
        this.currentCycle = client.cycles;
    }

    public void getAnimatedShadowedModel(AbstractToolkit toolkit) {
        getAnimatedModel(toolkit, SHADOWED, true, true);
    }
}
