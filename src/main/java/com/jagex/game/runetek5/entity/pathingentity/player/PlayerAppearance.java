package com.jagex.game.runetek5.entity.pathingentity.player;

import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.bas.BasTypeList;
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType;
import com.jagex.game.runetek5.config.identitykit.IdkTypeList;
import com.jagex.game.runetek5.config.npc.NpcTypeList;
import com.jagex.game.runetek5.config.obj.ObjTypeCustomisation;
import com.jagex.game.runetek5.config.obj.ObjTypeList;
import com.jagex.game.runetek5.config.sequence.SeqType;
import com.jagex.game.runetek5.config.sequence.Sequence;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.Hashing;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.vardomain.VarDomain;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

import com.jagex.game.runetek5.cache.node.lru.LRUCache;

/**
 * 
 * @author Brainpower - 20/05/2020
 *
 */
public class PlayerAppearance {
	/**
	 * Identity kit parts
	 */
    public static final int[] IDK_PART_TABLE = {8, 11, 4, 6, 9, 7, 10, 0};
    
    /**
     * Cached models
     */
    public static LRUCache bodyModelCache = new LRUCache(260);
    public static LRUCache headModelCache = new LRUCache(5);
    
    /**
     * Colors to find / replacements
     */
	public static short[][] recolorPaletteSrc;
	public static short[][][] recolorPaletteDst;
	
	/**
	 * Visual preferences (light / shadow)
	 */
	public static int preferenceFlags;
	
	/**
	 * Appearance hashes
	 */
    public long playerAppearanceHash;
    public long bodyModelHash;
	
	/**
	 * Appearance data
	 */
    public ObjTypeCustomisation[] meshModifiers;
    public int[] recolorDstIndices;
    public int morphism = -1;
    public boolean female;
    public int basId;
    public int[] appearance;

    public void setAppearance(int basId, int[] appearance, ObjTypeCustomisation[] meshModifiers, int[] recolorDst, boolean female, int morphism) {
        if (this.basId != basId) {
            this.basId = basId;
        }
        this.appearance = appearance;
        this.meshModifiers = meshModifiers;
        this.recolorDstIndices = recolorDst;
        this.female = female;
        this.morphism = morphism;
        calculateAppearanceHash();
    }

    public void addAppearanceIdentity(int slot, int id, IdkTypeList identityTypeList) {
        int partIndex = IDK_PART_TABLE[slot];
        if (identityTypeList.list(id) != null) {
            this.appearance[partIndex] = id | -2147483648;
            calculateAppearanceHash();
        }
    }

    public void addAppearanceItem(int index, int itemId, ObjTypeList objTypeList) {
        if (itemId == -1) {
            this.appearance[index] = 0;
        } else if (objTypeList.list(itemId) != null) {
            this.appearance[index] = itemId | 0x40000000;
            calculateAppearanceHash();
        }
    }
    
    public void changeGender(boolean female) {
        this.female = female;
        calculateAppearanceHash();
    }
    
    public void recolor(int slot, int dstColor) {
        this.recolorDstIndices[slot] = dstColor;
        calculateAppearanceHash();
    }
    
    
    
    /**
     * 839 [unused]
     */
    public long secondaryAppearanceHash;
   
    /**
	 * 839 [unused]
     */
    public Model getBodyModelRev839(AbstractToolkit toolkit, int initialFunctionMask, BasTypeList basTypeList, IdkTypeList idkTypeList, NpcTypeList loader, ObjTypeList objTypeList, VarDomain varProvider, Sequence animatingSequence, Sequence idleSequence, Sequence[] objSequences, int[] rotations, int rotationDelta, EquipmentDefaultsType equipmentDefaults) {
        if (this.morphism != -1) {
            return loader.list(this.morphism).getAnimatedBodymodel(toolkit, initialFunctionMask, basTypeList, varProvider, animatingSequence, idleSequence, objSequences, rotations, rotationDelta, null);
        } 
		boolean updateSecondaryAppearanceMask = true; //constant param 839 (true) --> boolean bool
		int displacementFunctionMask = initialFunctionMask;
		long PlayerAppearanceHash = this.playerAppearanceHash;
		int[] playerAppearance = this.appearance;
		boolean hasReplacementShield = false;
		boolean hasReplacementWeapon = false;
		if (animatingSequence != null) {
			SeqType animation = animatingSequence.getType();
			if (animation != null && (animation.replacementShield >= 0 || animation.replacementWeapon >= 0)) {
				playerAppearance = new int[this.appearance.length];
                System.arraycopy(this.appearance, 0, playerAppearance, 0, playerAppearance.length);
				if (animation.replacementShield >= 0 && -1 != equipmentDefaults.shieldSlot) {
					hasReplacementShield = true;
					if (animation.replacementShield == 0xffff) {
						playerAppearance[equipmentDefaults.shieldSlot] = 0;
						for (int slot = 0; slot < equipmentDefaults.hiddenAnimationShieldSlots.length; slot++) {
							playerAppearance[equipmentDefaults.hiddenAnimationShieldSlots[slot]] = 0;
						}
						PlayerAppearanceHash ^= ~0xffffffffL;
					} else {
						playerAppearance[equipmentDefaults.shieldSlot] = animation.replacementShield | 0x40000000;
						for (int slot = 0; slot < equipmentDefaults.hiddenAnimationShieldSlots.length; slot++) {
							playerAppearance[equipmentDefaults.hiddenAnimationShieldSlots[slot]] = 0;
						}
						PlayerAppearanceHash ^= (long) playerAppearance[equipmentDefaults.shieldSlot] << 32;
					}
				}
				if (animation.replacementWeapon >= 0 && -1 != equipmentDefaults.weaponSlot) {
					hasReplacementWeapon = true;
					if (animation.replacementWeapon == 0xffff) {
						playerAppearance[equipmentDefaults.weaponSlot] = 0;
						for (int slot = 0; slot < equipmentDefaults.hiddenAnimationWeaponSlots.length; slot++) {
							playerAppearance[equipmentDefaults.hiddenAnimationWeaponSlots[slot]] = 0;
						}
						PlayerAppearanceHash ^= 0xffffffffL;
					} else {
						playerAppearance[equipmentDefaults.weaponSlot] = animation.replacementWeapon | 0x40000000;
						for (int slot = 0; slot < equipmentDefaults.hiddenAnimationWeaponSlots.length; slot++) {
							playerAppearance[equipmentDefaults.hiddenAnimationWeaponSlots[slot]] = 0;
						}
						PlayerAppearanceHash ^= playerAppearance[equipmentDefaults.weaponSlot];
					}
				}
			}
		}
		boolean functionMaskChanged = false;
		int objSequenceCount = objSequences != null ? objSequences.length : 0;
		for (int index = 0; index < objSequenceCount; index++) {
			if (objSequences[index] != null) {
				displacementFunctionMask |= objSequences[index].getFunctionMask();
				functionMaskChanged = true;
			}
		}
		if (animatingSequence != null) {
			displacementFunctionMask |= animatingSequence.getFunctionMask();
			functionMaskChanged = true;
		}
		if (idleSequence != null) {
			displacementFunctionMask |= idleSequence.getFunctionMask();
			functionMaskChanged = true;
		}
		boolean requiresRotation = false;
		if (rotations != null) {
			for (int rotation : rotations) {
				if (rotation != -1) {
					displacementFunctionMask |= ALTERS_VERTICES;
					requiresRotation = true;
				}
			}
		}
		Model cachedModel;
		synchronized (bodyModelCache) {
			cachedModel = (Model) bodyModelCache.get(PlayerAppearanceHash);
		}
		BasType basType = null;
		if (this.basId != -1) {
			basType = basTypeList.list(this.basId);
		}
		if (cachedModel == null || toolkit.mismatchingFunctionMask(cachedModel.getFunctionMask(), displacementFunctionMask) != 0) {
			if (cachedModel != null) {
				displacementFunctionMask = toolkit.mergeFunctionMasks(displacementFunctionMask, cachedModel.getFunctionMask());
			}
			int adjustmentFunctionMask = displacementFunctionMask;
			boolean missingModels = false;
			for (int appearanceIndex = 0; appearanceIndex < playerAppearance.length; appearanceIndex++) {
				int appearancePart = playerAppearance[appearanceIndex];
				ObjTypeCustomisation meshModifier = null;
				boolean hiddenReplacements = false;
				if (hasReplacementShield) {
					if (appearanceIndex == equipmentDefaults.shieldSlot) {
						hiddenReplacements = true;
					} else {
						for (int slotToFind : equipmentDefaults.hiddenAnimationShieldSlots) {
							if (slotToFind == appearanceIndex) {
								hiddenReplacements = true;
								break;
							}
						}
					}
				}
				if (hasReplacementWeapon) {
					if (appearanceIndex == equipmentDefaults.weaponSlot) {
						hiddenReplacements = true;
					} else {
						for (int slotToFind : equipmentDefaults.hiddenAnimationWeaponSlots) {
							if (slotToFind == appearanceIndex) {
								hiddenReplacements = true;
								break;
							}
						}
					}
				}
				if ((appearancePart & 0x40000000) != 0) {
					if (!hiddenReplacements && this.meshModifiers != null && this.meshModifiers[appearanceIndex] != null) {
						meshModifier = this.meshModifiers[appearanceIndex];
					}
					if (!objTypeList.list(appearancePart & 0x3fffffff).isBodyCached(this.female, meshModifier)) {
						missingModels = true;
					}
				} else if ((appearancePart & ~0x7fffffff) != 0 && !idkTypeList.list(appearancePart & 0x3fffffff).isBodyCached()) {
					missingModels = true;
				}
			}
			if (missingModels) {
				if (this.secondaryAppearanceHash != -1L) {
					synchronized (bodyModelCache) {
						cachedModel = (Model) bodyModelCache.get(this.secondaryAppearanceHash);
					}
				}
				if (cachedModel == null || toolkit.mismatchingFunctionMask(cachedModel.getFunctionMask(), displacementFunctionMask) != 0) {
					return null;
				}
			} else {
				Mesh[] meshes = new Mesh[playerAppearance.length];
				for (int appearanceIndex = 0; appearanceIndex < playerAppearance.length; appearanceIndex++) {
					int appearancePart = playerAppearance[appearanceIndex];
					ObjTypeCustomisation meshModifier = null;
					boolean hasReplacements = appearanceIndex == 5 && hasReplacementShield || appearanceIndex == 3 && hasReplacementWeapon;
					if ((appearancePart & 0x40000000) != 0) {
						if (!hasReplacements && this.meshModifiers != null && this.meshModifiers[appearanceIndex] != null) {
							meshModifier = this.meshModifiers[appearanceIndex];
						}
						Mesh objMesh = objTypeList.list(appearancePart & 0x3fffffff).getBodyMesh(this.female, meshModifier);
						if (null != objMesh) {
							meshes[appearanceIndex] = objMesh;
						}
					} else if ((appearancePart & ~0x7fffffff) != 0) {
						Mesh identityMesh = idkTypeList.list(appearancePart & 0x3fffffff).getBodyMesh();
						if (null != identityMesh) {
							meshes[appearanceIndex] = identityMesh;
						}
					}
				}
				if (basType != null && basType.objVerticeTransformations != null) {
					for (int meshIndex = 0; meshIndex < basType.objVerticeTransformations.length; meshIndex++) {
						if (meshes[meshIndex] != null) {
							int translateX = 0;
							int translateY = 0;
							int translateZ = 0;
							int rotateX = 0;
							int rotateY = 0;
							int rotateZ = 0;
							if (null != basType.objVerticeTransformations[meshIndex]) {
								translateX = basType.objVerticeTransformations[meshIndex][0];
								translateY = basType.objVerticeTransformations[meshIndex][1];
								translateZ = basType.objVerticeTransformations[meshIndex][2];
								rotateX = basType.objVerticeTransformations[meshIndex][3] << 3;
								rotateY = basType.objVerticeTransformations[meshIndex][4] << 3;
								rotateZ = basType.objVerticeTransformations[meshIndex][5] << 3;
							}
							if (rotateX != 0 || rotateY != 0 || rotateZ != 0) {
								meshes[meshIndex].rotateWithoutNormals(rotateX, rotateY, rotateZ);
							}
							if (translateX != 0 || translateY != 0 || translateZ != 0) {
								meshes[meshIndex].translate(translateX, translateY, translateZ);
							}
						}
					}
				}
				Mesh combinedMeshes = new Mesh(meshes, meshes.length);
				adjustmentFunctionMask |= RECOLOR;
				cachedModel = toolkit.createModel(combinedMeshes, adjustmentFunctionMask, preferenceFlags, 64, 850);
                for (int recolorIndex = 0; recolorIndex < 10; recolorIndex++) {
                    for (int colorIndex = 0; colorIndex < recolorPaletteSrc[recolorIndex].length; colorIndex++) {
                        if (this.recolorDstIndices[recolorIndex] < recolorPaletteDst[recolorIndex][colorIndex].length) {
                        	cachedModel.recolor(recolorPaletteSrc[recolorIndex][colorIndex], recolorPaletteDst[recolorIndex][colorIndex][this.recolorDstIndices[recolorIndex]]);
                        }
                    }
                }
				
                // TODO: add support for this
                // 727 doesn't have this:
                // cached_model.method2962();
				if (updateSecondaryAppearanceMask) {
					cachedModel.applyShading(displacementFunctionMask);
					synchronized (bodyModelCache) {
						bodyModelCache.put(cachedModel, PlayerAppearanceHash);
					}
					this.secondaryAppearanceHash = PlayerAppearanceHash;
				}
			}
		}
		Model copiedModel = cachedModel.copy((byte) 4, displacementFunctionMask, true);
		if (!functionMaskChanged && !requiresRotation) {
			return copiedModel;
		}
		Matrix43[] submeshMatrices = null;
		if (basType != null) {
			submeshMatrices = basType.getSubmeshMatrices();
		}
		if (requiresRotation && null != submeshMatrices) {
			for (int index = 0; index < rotations.length; index++) {
				if (submeshMatrices[index] != null) {
					copiedModel.offsetSubmesh(submeshMatrices[index], 1 << index, true);
				}
			}
		}
		// TODO: make this cleaner
		int seqIndex = 0;
		int submesh = 1;
		while (seqIndex < objSequenceCount) {
			if (null != objSequences[seqIndex]) {
				objSequences[seqIndex].animateUnrotatedModel(copiedModel, /*0,*/ submesh);
			}
			seqIndex++;
			submesh <<= 1;
		}
		if (requiresRotation) {
			for (int rotationIndex = 0; rotationIndex < rotations.length; rotationIndex++) {
				if (-1 != rotations[rotationIndex]) {
					int rotation = rotations[rotationIndex] - rotationDelta;
					rotation &= 0x3fff;
					Matrix43 matrix = new Matrix43();
					matrix.rotateMatrix(0.0F, 1.0F, 0.0F, MathUtils.getTheta(rotation));
					copiedModel.offsetSubmesh(matrix, 1 << rotationIndex, false);
				}
			}
		}
		if (requiresRotation && submeshMatrices != null) {
			for (int matriceIndex = 0; matriceIndex < rotations.length; matriceIndex++) {
				if (submeshMatrices[matriceIndex] != null) {
					copiedModel.offsetSubmesh(submeshMatrices[matriceIndex], 1 << matriceIndex, false);
				}
			}
		}
		if (animatingSequence != null && null != idleSequence) {
			Sequence.doubleAnimation(copiedModel, animatingSequence, idleSequence);
		} else if (null != animatingSequence) {
			animatingSequence.animateRotatedModel(copiedModel, 0);
		} else if (null != idleSequence) {
			idleSequence.animateRotatedModel(copiedModel, 0);
		}
		return copiedModel;
	}

    // 727
    public Model getBodyModel(AbstractToolkit toolkit, int initialFunctionMask, BasTypeList basTypeList, IdkTypeList idkTypeList, NpcTypeList loader, ObjTypeList objTypeList, VarDomain varProvider, Sequence animatingSequence, Sequence idleSequence, Sequence[] objSequence, int[] rotations, int rotationDelta, EquipmentDefaultsType equipmentDefaults) {
        if (this.morphism != -1) {
            return loader.list(this.morphism).getAnimatedBodymodel(toolkit, initialFunctionMask, basTypeList, varProvider, animatingSequence, idleSequence, objSequence, rotations, rotationDelta, null);
        } 
        int displacementFunctionMask = initialFunctionMask;
        long AppearanceHash = this.playerAppearanceHash;
        int[] playerAppearance = this.appearance;
        boolean hasReplacementShield = false;
        boolean hasReplacementWeapon = false;
        if (animatingSequence != null) {
            SeqType seqType = animatingSequence.getType();
            if (seqType != null && (seqType.replacementShield >= 0 || seqType.replacementWeapon >= 0)) {
                playerAppearance = new int[this.appearance.length];
                System.arraycopy(this.appearance, 0, playerAppearance, 0, playerAppearance.length);
                if (seqType.replacementShield >= 0 && equipmentDefaults.shieldSlot != -1) {
                    hasReplacementShield = true;
                    if (seqType.replacementShield == 65535) {
                        playerAppearance[equipmentDefaults.shieldSlot] = 0;
                        for (int index = 0; index < equipmentDefaults.hiddenAnimationShieldSlots.length; index++) {
                            playerAppearance[equipmentDefaults.hiddenAnimationShieldSlots[index]] = 0;
                        }
                        AppearanceHash ^= -4294967296L;
                    } else {
                        playerAppearance[equipmentDefaults.shieldSlot] = seqType.replacementShield | 0x40000000;
                        for (int index = 0; index < equipmentDefaults.hiddenAnimationShieldSlots.length; index++) {
                            playerAppearance[equipmentDefaults.hiddenAnimationShieldSlots[index]] = 0;
                        }
                        AppearanceHash ^= (long) playerAppearance[equipmentDefaults.shieldSlot] << 32;
                    }
                }
                if (seqType.replacementWeapon >= 0 && equipmentDefaults.weaponSlot != -1) {
                    hasReplacementWeapon = true;
                    if (seqType.replacementWeapon == 65535) {
                        playerAppearance[equipmentDefaults.weaponSlot] = 0;
                        for (int index = 0; index < equipmentDefaults.hiddenAnimationWeaponSlots.length; index++) {
                            playerAppearance[equipmentDefaults.hiddenAnimationWeaponSlots[index]] = 0;
                        }
                        AppearanceHash ^= 0xffffffffL;
                    } else {
                        playerAppearance[equipmentDefaults.weaponSlot] = seqType.replacementWeapon | 0x40000000;
                        for (int index = 0; index < equipmentDefaults.hiddenAnimationWeaponSlots.length; index++) {
                            playerAppearance[equipmentDefaults.hiddenAnimationWeaponSlots[index]] = 0;
                        }
                        AppearanceHash ^= playerAppearance[equipmentDefaults.weaponSlot];
                    }
                }
            }
        }
        boolean flagAdjustment = false;
        int index = objSequence != null ? objSequence.length : 0;
        for (int animationIndex = 0; animationIndex < index; animationIndex++) {
            if (objSequence[animationIndex] != null) {
                displacementFunctionMask |= objSequence[animationIndex].getFunctionMask();
                flagAdjustment = true;
            }
        }
        if (animatingSequence != null) {
            displacementFunctionMask |= animatingSequence.getFunctionMask();
            flagAdjustment = true;
        }
        if (idleSequence != null) {
            displacementFunctionMask |= idleSequence.getFunctionMask();
            flagAdjustment = true;
        }
        boolean requiresRotation = false;
        if (rotations != null) {
            for (int rotationSlot = 0; rotationSlot < rotations.length; rotationSlot++) {
                if (rotations[rotationSlot] != -1) {
                    displacementFunctionMask |= 0x20;
                    requiresRotation = true;
                }
            }
        }
        Model model;
        synchronized (bodyModelCache) {
            model = (Model) bodyModelCache.get(AppearanceHash);
        }
        BasType basType = null;
        if (this.basId != -1) {
            basType = basTypeList.list(this.basId);
        }
        if (model == null || toolkit.mismatchingFunctionMask(model.getFunctionMask(), displacementFunctionMask) != 0) {
            if (model != null) {
                displacementFunctionMask = toolkit.mergeFunctionMasks(displacementFunctionMask, model.getFunctionMask());
            }
            boolean missingModels = false;
            for(int currentSlot = 0; /**/ ; currentSlot++) {
                if (currentSlot >= playerAppearance.length) {
                    if (missingModels) {
                        if (this.bodyModelHash != -1L) {
                            synchronized (bodyModelCache) {
                                model = (Model) bodyModelCache.get(this.bodyModelHash);
                            }
                        }
                        if (model == null || toolkit.mergeFunctionMasks(model.getFunctionMask(), displacementFunctionMask) != 0) {
                            return null;
                        }
                    } else {
                        Mesh[] meshesToCombine = new Mesh[playerAppearance.length];
                        for (int slot = 0; slot < playerAppearance.length; slot++) {
                        	int bitpackedAppearancePart = playerAppearance[slot];
                            ObjTypeCustomisation meshModifier = null;
                            boolean objReplacement = slot == 5 && hasReplacementShield || slot == 3 && hasReplacementWeapon;
                            if ((bitpackedAppearancePart & 0x40000000) != 0) {
                                if (!objReplacement && this.meshModifiers != null && this.meshModifiers[slot] != null) {
                                    meshModifier = this.meshModifiers[slot];
                                }
                                Mesh objBodyMesh = objTypeList.list(bitpackedAppearancePart & 0x3fffffff).getBodyMesh(this.female, meshModifier);
                                if (objBodyMesh != null) {
                                    meshesToCombine[slot] = objBodyMesh;
                                }
                            } else if ((bitpackedAppearancePart & -2147483648) != 0) {
                            	Mesh idkBodyMesh = idkTypeList.list(bitpackedAppearancePart & 0x3fffffff).getBodyMesh();
                                if (idkBodyMesh != null) {
                                    meshesToCombine[slot] = idkBodyMesh;
                                }
                            }
                        }
                        if (basType != null && basType.objVerticeTransformations != null) {
                            for (int matrixIndex = 0; matrixIndex < basType.objVerticeTransformations.length; matrixIndex++) {
                                if (meshesToCombine[matrixIndex] != null) {
                                	int translateX = 0;
                                	int translateY = 0;
                                    int translateZ = 0;
                                    int rotateX = 0;
                                    int rotateY = 0;
                                    int rotateZ = 0;
                                    if (basType.objVerticeTransformations[matrixIndex] != null) {
                                        translateX = basType.objVerticeTransformations[matrixIndex][0];
                                        translateY = basType.objVerticeTransformations[matrixIndex][1];
                                        translateZ = basType.objVerticeTransformations[matrixIndex][2];
                                        rotateX = basType.objVerticeTransformations[matrixIndex][3] << 3;
                                        rotateY = basType.objVerticeTransformations[matrixIndex][4] << 3;
                                        rotateZ = basType.objVerticeTransformations[matrixIndex][5] << 3;
                                    }
                                    if (rotateX != 0 || rotateY != 0 || rotateZ != 0) {
                                        meshesToCombine[matrixIndex].rotateWithoutNormals(rotateX, rotateY, rotateZ);
                                    }
                                    if (translateX != 0 || translateY != 0 || translateZ != 0) {
                                        meshesToCombine[matrixIndex].translate(translateX, translateY, translateZ);
                                    }
                                }
                            }
                        }
                        Mesh combinedMesh = new Mesh(meshesToCombine, meshesToCombine.length);
                        int adjustmentFunctionMask = displacementFunctionMask | 0x4000;
                        model = toolkit.createModel(combinedMesh, adjustmentFunctionMask, PlayerAppearance.preferenceFlags, 64, 850);
                        for (int recolorIndex = 0; recolorIndex < 10; recolorIndex++) {
                            for (int colorIndex = 0; colorIndex < recolorPaletteSrc[recolorIndex].length; colorIndex++) {
                                if (this.recolorDstIndices[recolorIndex] < recolorPaletteDst[recolorIndex][colorIndex].length) {
                                    model.recolor(recolorPaletteSrc[recolorIndex][colorIndex], recolorPaletteDst[recolorIndex][colorIndex][this.recolorDstIndices[recolorIndex]]);
                                }
                            }
                        }
                        model.applyShading(displacementFunctionMask);
                        synchronized (bodyModelCache) {
                        	bodyModelCache.put(model, AppearanceHash);
                        }
                        this.bodyModelHash = AppearanceHash;
                    }
                    break;
                }
                int bitpackedAppearancePart = playerAppearance[currentSlot];
                boolean slotObjReplaced = false;
                if (hasReplacementShield) {
                    if (equipmentDefaults.shieldSlot == currentSlot) {
                        slotObjReplaced = true;
                    } else {
                        for (int slotIndex = 0; slotIndex < equipmentDefaults.hiddenAnimationShieldSlots.length; slotIndex++) {
                            if (equipmentDefaults.hiddenAnimationShieldSlots[slotIndex] == currentSlot) {
                                slotObjReplaced = true;
                                break;
                            }
                        }
                    }
                }
                if (hasReplacementWeapon) {
                    if (equipmentDefaults.weaponSlot == currentSlot) {
                        slotObjReplaced = true;
                    } else {
                        for (int slotIndex = 0; slotIndex < equipmentDefaults.hiddenAnimationWeaponSlots.length; slotIndex++) {
                            if (equipmentDefaults.hiddenAnimationWeaponSlots[slotIndex] == currentSlot) {
                                slotObjReplaced = true;
                                break;
                            }
                        }
                    }
                }
                if ((bitpackedAppearancePart & 0x40000000) != 0) {
                	ObjTypeCustomisation meshModifier = null;
                    if (!slotObjReplaced && this.meshModifiers != null && this.meshModifiers[currentSlot] != null) {
                        meshModifier = this.meshModifiers[currentSlot];
                    }
                    if (!objTypeList.list(bitpackedAppearancePart & 0x3fffffff).isBodyCached(this.female, meshModifier)) {
                        missingModels = true;
                    }
                } else if ((bitpackedAppearancePart & -2147483648) != 0 && !idkTypeList.list(bitpackedAppearancePart & 0x3fffffff).isBodyCached()) {
                    missingModels = true;
                }
            }
        }
        Model copiedModel = model.copy((byte) 4, displacementFunctionMask, true);
        if (flagAdjustment || requiresRotation) {
            Matrix43[] submeshMatrices = null;
            if (basType != null) {
                submeshMatrices = basType.getSubmeshMatrices();
            }
            if (requiresRotation && submeshMatrices != null) {
                for (int submeshIndex = 0; submeshIndex < rotations.length; submeshIndex++) {
                    if (submeshMatrices[submeshIndex] != null) {
                        copiedModel.offsetSubmesh(submeshMatrices[submeshIndex], 1 << submeshIndex, true);
                    }
                }
            }
            int sequenceIndex = 0;
            for (int submesh = 1; sequenceIndex < index; submesh <<= 1) {
                if (objSequence[sequenceIndex] != null) {
                    objSequence[sequenceIndex].animateUnrotatedModel(copiedModel, submesh);
                }
                sequenceIndex++;
            }
            if (requiresRotation) {
                for (int rotationIndex = 0; rotationIndex < rotations.length; rotationIndex++) {
                    if (rotations[rotationIndex] != -1) {
                    	int rotation = rotations[rotationIndex] - rotationDelta;
                        rotation &= 0x3fff;
                        Matrix43 rotatedMatrix = new Matrix43();
                        rotatedMatrix.rotateMatrix(0.0F, 1.0F, 0.0F, MathUtils.getTheta(rotation));
                        copiedModel.offsetSubmesh(rotatedMatrix, 1 << rotationIndex, false);
                    }
                }
            }
            if (requiresRotation && submeshMatrices != null) {
                for (int matrixIndex = 0; matrixIndex < rotations.length; matrixIndex++) {
                    if (submeshMatrices[matrixIndex] != null) {
                        copiedModel.offsetSubmesh(submeshMatrices[matrixIndex], 1 << matrixIndex, false);
                    }
                }
            }
            if (animatingSequence != null && idleSequence != null) {
                Sequence.doubleAnimation(copiedModel, animatingSequence, idleSequence);
            } else if (animatingSequence != null) {
                animatingSequence.animateRotatedModel(copiedModel, 0);
            } else if (idleSequence != null) {
                idleSequence.animateRotatedModel(copiedModel, 0);
            }
        }
        return copiedModel;
    }

    /**
     * Gets the head model of a player without any equipment displayed
     */
    public Model getPlainAnimatedHeadModel(AbstractToolkit toolkit, int initialFunctionMask, IdkTypeList identityTypeList, Sequence sequence, int primaryHead, int secondaryHead, int tertiaryHead) {
        int displacementFunctionMask = sequence != null ? initialFunctionMask | sequence.getFunctionMask() : initialFunctionMask;
        long headUid = (long) tertiaryHead << 32 | ((long) secondaryHead << 16) | primaryHead;
        Model model;
        synchronized (headModelCache) {
            model = (Model) headModelCache.get(headUid);
        }
        if (model == null || toolkit.mismatchingFunctionMask(model.getFunctionMask(), displacementFunctionMask) != 0) {
            if (model != null) {
                displacementFunctionMask = toolkit.mergeFunctionMasks(displacementFunctionMask, model.getFunctionMask());
            }
            Mesh[] headMeshes = new Mesh[3];
            int headParts = 0;
            if (!identityTypeList.list(primaryHead).isHeadCached() || !identityTypeList.list(secondaryHead).isHeadCached() || !identityTypeList.list(tertiaryHead).isHeadCached()) {
                return null;
            }
            Mesh primaryHeadMesh = identityTypeList.list(primaryHead).getHeadMesh();
            if (primaryHeadMesh != null) {
                headMeshes[headParts++] = primaryHeadMesh;
            }
            Mesh secondaryHeadMesh = identityTypeList.list(secondaryHead).getHeadMesh();
            if (secondaryHeadMesh != null) {
                headMeshes[headParts++] = secondaryHeadMesh;
            }
            Mesh tertiaryHeadMesh = identityTypeList.list(tertiaryHead).getHeadMesh();
            if (tertiaryHeadMesh != null) {
                headMeshes[headParts++] = tertiaryHeadMesh;
            }
            Mesh combinedHeadMesh = new Mesh(headMeshes, headParts);
            int adjustmentFunctionMask = displacementFunctionMask | RECOLOR;
            model = toolkit.createModel(combinedHeadMesh, adjustmentFunctionMask, preferenceFlags, 64, 768);
            for (int recolorIndex = 0; recolorIndex < 10; recolorIndex++) {
                for (int colorIndex = 0; colorIndex < recolorPaletteSrc[recolorIndex].length; colorIndex++) {
                    if (this.recolorDstIndices[recolorIndex] < recolorPaletteDst[recolorIndex][colorIndex].length) {
                        model.recolor(recolorPaletteSrc[recolorIndex][colorIndex], recolorPaletteDst[recolorIndex][colorIndex][this.recolorDstIndices[recolorIndex]]);
                    }
                }
            }
            model.applyShading(displacementFunctionMask);
            synchronized (headModelCache) {
                headModelCache.put(model, headUid);
            }
        }
        if (sequence != null) {
            model = model.copy((byte) 4, displacementFunctionMask, true);
            sequence.animateRotatedModel(model, 0);
        }
        return model;
    }

    /**
     * Gets the head model of a player with equipment displayed
     */
    public Model getAnimatedHeadModel(AbstractToolkit toolkit, int initialFunctionMask, IdkTypeList idkTypeList, NpcTypeList npcTypeList, ObjTypeList objTypeList, VarDomain varProvider, Sequence sequence) {
        if (this.morphism != -1) {
            return npcTypeList.list(this.morphism).getAnimatedHeadmodel(toolkit, initialFunctionMask, varProvider, sequence, null);
        } 
        int displacementFunctionMask = sequence != null ? initialFunctionMask | sequence.getFunctionMask() : initialFunctionMask;
        Model model;
        synchronized (headModelCache) {
            model = (Model) headModelCache.get(this.playerAppearanceHash);
        }
        if (model == null || toolkit.mismatchingFunctionMask(model.getFunctionMask(), displacementFunctionMask) != 0) {
            if (model != null) {
                displacementFunctionMask = toolkit.mergeFunctionMasks(displacementFunctionMask, model.getFunctionMask());
            }
            boolean missingModels = false;
            for (int index = 0; index < this.appearance.length; index++) {
                int appearancePart = this.appearance[index];
                ObjTypeCustomisation meshModifier = null;
                if ((appearancePart & 0x40000000) != 0) {
                    if (this.meshModifiers != null && this.meshModifiers[index] != null) {
                        meshModifier = this.meshModifiers[index];
                    }
                    if (!objTypeList.list(appearancePart & 0x3fffffff).isHeadCached(this.female, meshModifier)) {
                        missingModels = true;
                    }
                } else if ((appearancePart & -2147483648) != 0 && !idkTypeList.list(appearancePart & 0x3fffffff).isHeadCached()) {
                    missingModels = true;
                }
            }
            if (missingModels) {
                return null;
            }
            Mesh[] meshesToCombine = new Mesh[this.appearance.length];
            int meshCount = 0;
            for (int index = 0; index < this.appearance.length; index++) {
                int appearancePart = this.appearance[index];
                if ((appearancePart & 0x40000000) != 0) {
                    ObjTypeCustomisation meshModifier = null;
                    if (this.meshModifiers != null && this.meshModifiers[index] != null) {
                        meshModifier = this.meshModifiers[index];
                    }
                    Mesh objMesh = objTypeList.list(appearancePart & 0x3fffffff).getHeadMesh(this.female, meshModifier);
                    if (objMesh != null) {
                        meshesToCombine[meshCount++] = objMesh;
                    }
                } else if ((appearancePart & -2147483648) != 0) {
                	Mesh idkMesh = idkTypeList.list(appearancePart & 0x3fffffff).getHeadMesh();
                    if (idkMesh != null) {
                        meshesToCombine[meshCount++] = idkMesh;
                    }
                }
            }
            Mesh headMesh = new Mesh(meshesToCombine, meshCount);
            int adjustmentFunctionMask = displacementFunctionMask | RECOLOR;
            model = toolkit.createModel(headMesh, adjustmentFunctionMask, preferenceFlags, 64, 768);
            for (int recolorIndex = 0; recolorIndex < 10; recolorIndex++) {
                for (int colorIndex = 0; colorIndex < PlayerAppearance.recolorPaletteSrc[recolorIndex].length; colorIndex++) {
                    if (this.recolorDstIndices[recolorIndex] < recolorPaletteDst[recolorIndex][colorIndex].length) {
                    	model.recolor(recolorPaletteSrc[recolorIndex][colorIndex], recolorPaletteDst[recolorIndex][colorIndex][this.recolorDstIndices[recolorIndex]]);
                    }
                }
            }
            model.applyShading(displacementFunctionMask);
            synchronized (headModelCache) {
                headModelCache.put(model, this.playerAppearanceHash);
            }
        }
        if (sequence == null) {
            return model;
        } 
        Model animatedModel = model.copy((byte) 4, displacementFunctionMask, true);
        sequence.animateRotatedModel(animatedModel, 0);
        return animatedModel;
    }
    
    public void calculateAppearanceHash() {
        long[] crc64 = Hashing.CRC64_TABLE;
        this.playerAppearanceHash = -1L;
        this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ (this.basId >> 8)) & 0xffL)];
        this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ this.basId) & 0xffL)];
        for (int part = 0; part < this.appearance.length; part++) {
            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ (this.appearance[part] >> 24)) & 0xffL)];
            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ (this.appearance[part] >> 16)) & 0xffL)];
            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ (this.appearance[part] >> 8)) & 0xffL)];
            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ this.appearance[part]) & 0xffL)];
        }
        if (this.meshModifiers != null) {
            for (int index = 0; index < this.meshModifiers.length; index++) {
                if (this.meshModifiers[index] != null) {
                    int[] body;
                    int[] head;
                    if (this.female) {
                        body = this.meshModifiers[index].femaleBody;
                        head = this.meshModifiers[index].femaleHead;
                    } else {
                        body = this.meshModifiers[index].maleBody;
                        head = this.meshModifiers[index].maleHead;
                    }
                    if (body != null) {
                        for (int part = 0; part < body.length; part++) {
                            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ (body[part] >> 8)) & 0xffL)];
                            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ body[part]) & 0xffL)];
                        }
                    }
                    if (head != null) {
                        for (int part = 0; part < head.length; part++) {
                            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ (head[part] >> 8)) & 0xffL)];
                            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ head[part]) & 0xffL)];
                        }
                    }
                    if (this.meshModifiers[index].recolDst != null) {
                        for (int part = 0; part < this.meshModifiers[index].recolDst.length; part++) {
                            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ (this.meshModifiers[index].recolDst[part] >> 8)) & 0xffL)];
                            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ this.meshModifiers[index].recolDst[part]) & 0xffL)];
                        }
                    }
                    if (this.meshModifiers[index].retexDst != null) {
                        for (int part = 0; part < this.meshModifiers[index].retexDst.length; part++) {
                            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ (this.meshModifiers[index].retexDst[part] >> 8)) & 0xffL)];
                            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ this.meshModifiers[index].retexDst[part]) & 0xffL)];
                        }
                    }
                }
            }
        }
        for (int part = 0; part < 10; part++) {
            this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ this.recolorDstIndices[part]) & 0xffL)];
        }
        this.playerAppearanceHash = this.playerAppearanceHash >>> 8 ^ crc64[(int) ((this.playerAppearanceHash ^ (this.female ? 1 : 0)) & 0xffL)];
    }

	public static void processCacheCycle() {
	    synchronized (bodyModelCache) {
	        bodyModelCache.clean(5);
	    }
	    synchronized (headModelCache) {
	        headModelCache.clean(5);
	    }
	}
	
    public static int countBodyModels() {
        synchronized (bodyModelCache) {
            int count = bodyModelCache.count();
            return count;
        }
    }

    public static void resetCache() {
        synchronized (bodyModelCache) {
            bodyModelCache.reset();
        }
        synchronized (headModelCache) {
            headModelCache.reset();
        }
    }

    public static void clearQueue() {
        synchronized (bodyModelCache) {
            bodyModelCache.resize();
        }
        synchronized (headModelCache) {
            headModelCache.resize();
        }
    }

    public static void setPreferences(int prefFlags) {
        PlayerAppearance.preferenceFlags = prefFlags;
        synchronized (headModelCache) {
            headModelCache.reset();
        }
        synchronized (bodyModelCache) {
            bodyModelCache.reset();
        }
    }
}
