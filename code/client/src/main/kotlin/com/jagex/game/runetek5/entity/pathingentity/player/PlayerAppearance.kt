package com.jagex.game.runetek5.entity.pathingentity.player

import com.jagex.game.runetek5.config.bas.BasType
import com.jagex.game.runetek5.config.bas.BasTypeList
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType
import com.jagex.game.runetek5.config.identitykit.IdkTypeList
import com.jagex.game.runetek5.config.npc.NpcTypeList
import com.jagex.game.runetek5.config.item.ItemTypeCustomisation
import com.jagex.game.runetek5.config.item.ItemTypeList
import com.jagex.game.runetek5.config.sequence.SeqType
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelGL
import com.jagex.game.runetek5.graphics.model.PoseCopyPair
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.Hashing
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.vardomain.VarDomain
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.cache.node.lru.LRUCache

class PlayerAppearance {
	@JvmField
	var playerAppearanceHash: Long = 0L
	@JvmField
	var bodyModelHash: Long = 0L

	@JvmField
	var meshModifiers: Array<ItemTypeCustomisation>? = null
	@JvmField
	var recolorDstIndices: IntArray? = null
	@JvmField
	var morphism: Int = -1
	@JvmField
	var female: Boolean = false
	@JvmField
	var basId: Int = 0
	@JvmField
	var appearance: IntArray? = null

	@JvmField
	var secondaryAppearanceHash: Long = 0L

	fun setAppearance(basId: Int, appearance: IntArray, meshModifiers: Array<ItemTypeCustomisation>?, recolorDst: IntArray, female: Boolean, morphism: Int) {
		if (this.basId != basId) {
			this.basId = basId
		}
		this.appearance = appearance
		this.meshModifiers = meshModifiers
		this.recolorDstIndices = recolorDst
		this.female = female
		this.morphism = morphism
		calculateAppearanceHash()
	}

	fun addAppearanceIdentity(slot: Int, id: Int, identityTypeList: IdkTypeList) {
		val partIndex = IDK_PART_TABLE[slot]
		if (identityTypeList.list(id) != null) {
			appearance!![partIndex] = id or -2147483648
			calculateAppearanceHash()
		}
	}

	fun addAppearanceItem(index: Int, itemId: Int, objTypeList: ItemTypeList) {
		if (itemId == -1) {
			appearance!![index] = 0
		} else if (objTypeList.list(itemId) != null) {
			appearance!![index] = itemId or 0x40000000
			calculateAppearanceHash()
		}
	}

	fun changeGender(female: Boolean) {
		this.female = female
		calculateAppearanceHash()
	}

	fun recolor(slot: Int, dstColor: Int) {
		recolorDstIndices!![slot] = dstColor
		calculateAppearanceHash()
	}

	fun getBodyModelRev839(toolkit: AbstractToolkit, initialFunctionMask: Int, basTypeList: BasTypeList, idkTypeList: IdkTypeList, loader: NpcTypeList, objTypeList: ItemTypeList, varProvider: VarDomain, animatingSequence: Sequence?, idleSequence: Sequence?, objSequences: Array<Sequence?>?, rotations: IntArray?, rotationDelta: Int, equipmentDefaults: EquipmentDefaultsType): Model? {
		if (this.morphism != -1) {
			return loader.list(this.morphism).getAnimatedBodymodel(toolkit, initialFunctionMask, basTypeList, varProvider, animatingSequence, idleSequence, objSequences, rotations, rotationDelta, null)
		}
		val updateSecondaryAppearanceMask = true
		var displacementFunctionMask = initialFunctionMask
		var PlayerAppearanceHash = this.playerAppearanceHash
		var playerAppearance = this.appearance
		var hasReplacementShield = false
		var hasReplacementWeapon = false
		if (animatingSequence != null) {
			val animation = animatingSequence.getType()
			if (animation != null && (animation.replacementShield >= 0 || animation.replacementWeapon >= 0)) {
				playerAppearance = IntArray(this.appearance!!.size)
				System.arraycopy(this.appearance!!, 0, playerAppearance, 0, playerAppearance.size)
				if (animation.replacementShield >= 0 && -1 != equipmentDefaults.shieldSlot) {
					hasReplacementShield = true
					if (animation.replacementShield == 0xffff) {
						playerAppearance[equipmentDefaults.shieldSlot] = 0
						for (slot in equipmentDefaults.hiddenAnimationShieldSlots!!.indices) {
							playerAppearance[equipmentDefaults.hiddenAnimationShieldSlots!![slot]] = 0
						}
						PlayerAppearanceHash = PlayerAppearanceHash xor -0x1L
					} else {
						playerAppearance[equipmentDefaults.shieldSlot] = animation.replacementShield or 0x40000000
						for (slot in equipmentDefaults.hiddenAnimationShieldSlots!!.indices) {
							playerAppearance[equipmentDefaults.hiddenAnimationShieldSlots!![slot]] = 0
						}
						PlayerAppearanceHash = PlayerAppearanceHash xor (playerAppearance[equipmentDefaults.shieldSlot].toLong() shl 32)
					}
				}
				if (animation.replacementWeapon >= 0 && -1 != equipmentDefaults.weaponSlot) {
					hasReplacementWeapon = true
					if (animation.replacementWeapon == 0xffff) {
						playerAppearance[equipmentDefaults.weaponSlot] = 0
						for (slot in equipmentDefaults.hiddenAnimationWeaponSlots!!.indices) {
							playerAppearance[equipmentDefaults.hiddenAnimationWeaponSlots!![slot]] = 0
						}
						PlayerAppearanceHash = PlayerAppearanceHash xor 0xffffffffL
					} else {
						playerAppearance[equipmentDefaults.weaponSlot] = animation.replacementWeapon or 0x40000000
						for (slot in equipmentDefaults.hiddenAnimationWeaponSlots!!.indices) {
							playerAppearance[equipmentDefaults.hiddenAnimationWeaponSlots!![slot]] = 0
						}
						PlayerAppearanceHash = PlayerAppearanceHash xor playerAppearance[equipmentDefaults.weaponSlot].toLong()
					}
				}
			}
		}
		var functionMaskChanged = false
		val objSequenceCount = objSequences?.size ?: 0
		for (index in 0 until objSequenceCount) {
			if (objSequences!![index] != null) {
				displacementFunctionMask = displacementFunctionMask or objSequences[index]!!.getFunctionMask()
				functionMaskChanged = true
			}
		}
		if (animatingSequence != null) {
			displacementFunctionMask = displacementFunctionMask or animatingSequence.getFunctionMask()
			functionMaskChanged = true
		}
		if (idleSequence != null) {
			displacementFunctionMask = displacementFunctionMask or idleSequence.getFunctionMask()
			functionMaskChanged = true
		}
		var requiresRotation = false
		if (rotations != null) {
			for (rotation in rotations!!) {
				if (rotation != -1) {
					displacementFunctionMask = displacementFunctionMask or ModelFunctionMask.ALTERS_VERTICES
					requiresRotation = true
				}
			}
		}
		var cachedModel: Model?
		synchronized(bodyModelCache) {
			cachedModel = bodyModelCache.get(PlayerAppearanceHash) as Model?
		}
		var basType: BasType? = null
		if (this.basId != -1) {
			basType = basTypeList.list(this.basId)
		}
		if (cachedModel == null || toolkit.mismatchingFunctionMask(cachedModel!!.getFunctionMask(), displacementFunctionMask) != 0) {
			if (cachedModel != null) {
				displacementFunctionMask = toolkit.mergeFunctionMasks(displacementFunctionMask, cachedModel.getFunctionMask())
			}
			var adjustmentFunctionMask = displacementFunctionMask
			var missingModels = false
			for (appearanceIndex in playerAppearance!!.indices) {
				val appearancePart = playerAppearance[appearanceIndex]
				var meshModifier: ItemTypeCustomisation? = null
				var hiddenReplacements = false
				if (hasReplacementShield) {
					if (appearanceIndex == equipmentDefaults.shieldSlot) {
						hiddenReplacements = true
					} else {
						for (slotToFind in equipmentDefaults.hiddenAnimationShieldSlots!!) {
							if (slotToFind == appearanceIndex) {
								hiddenReplacements = true
								break
							}
						}
					}
				}
				if (hasReplacementWeapon) {
					if (appearanceIndex == equipmentDefaults.weaponSlot) {
						hiddenReplacements = true
					} else {
						for (slotToFind in equipmentDefaults.hiddenAnimationWeaponSlots!!) {
							if (slotToFind == appearanceIndex) {
								hiddenReplacements = true
								break
							}
						}
					}
				}
				if ((appearancePart and 0x40000000) != 0) {
					if (!hiddenReplacements && this.meshModifiers != null && this.meshModifiers!![appearanceIndex] != null) {
						meshModifier = this.meshModifiers!![appearanceIndex]
					}
					if (!objTypeList.list(appearancePart and 0x3fffffff).isBodyCached(this.female, meshModifier)) {
						missingModels = true
					}
				} else if ((appearancePart and -0x80000000) != 0 && !idkTypeList.list(appearancePart and 0x3fffffff).isBodyCached()) {
					missingModels = true
				}
			}
			if (missingModels) {
				if (this.secondaryAppearanceHash != -1L) {
					synchronized(bodyModelCache) {
						cachedModel = bodyModelCache.get(this.secondaryAppearanceHash) as Model?
					}
				}
				if (cachedModel == null || toolkit.mismatchingFunctionMask(cachedModel!!.getFunctionMask(), displacementFunctionMask) != 0) {
					return null
				}
			} else {
				val meshes = arrayOfNulls<Mesh?>(playerAppearance.size)
				for (appearanceIndex in playerAppearance.indices) {
					val appearancePart = playerAppearance[appearanceIndex]
					var meshModifier: ItemTypeCustomisation? = null
					val hasReplacements = appearanceIndex == 5 && hasReplacementShield || appearanceIndex == 3 && hasReplacementWeapon
					if ((appearancePart and 0x40000000) != 0) {
						if (!hasReplacements && this.meshModifiers != null && this.meshModifiers!![appearanceIndex] != null) {
							meshModifier = this.meshModifiers!![appearanceIndex]
						}
						val objMesh = objTypeList.list(appearancePart and 0x3fffffff).getBodyMesh(this.female, meshModifier)
						if (null != objMesh) {
							meshes[appearanceIndex] = objMesh
						}
					} else if ((appearancePart and -0x80000000) != 0) {
						val identityMesh = idkTypeList.list(appearancePart and 0x3fffffff).getBodyMesh()
						if (null != identityMesh) {
							meshes[appearanceIndex] = identityMesh
						}
					}
				}
				if (basType != null && basType.objVerticeTransformations != null) {
					for (meshIndex in basType.objVerticeTransformations!!.indices) {
						if (meshes[meshIndex] != null) {
							var translateX = 0
							var translateY = 0
							var translateZ = 0
							var rotateX = 0
							var rotateY = 0
							var rotateZ = 0
							if (null != basType.objVerticeTransformations!![meshIndex]) {
								translateX = basType.objVerticeTransformations!![meshIndex]!![0]
								translateY = basType.objVerticeTransformations!![meshIndex]!![1]
								translateZ = basType.objVerticeTransformations!![meshIndex]!![2]
								rotateX = basType.objVerticeTransformations!![meshIndex]!![3] shl 3
								rotateY = basType.objVerticeTransformations!![meshIndex]!![4] shl 3
								rotateZ = basType.objVerticeTransformations!![meshIndex]!![5] shl 3
							}
							if (rotateX != 0 || rotateY != 0 || rotateZ != 0) {
								meshes[meshIndex]!!.rotateWithoutNormals(rotateX, rotateY, rotateZ)
							}
							if (translateX != 0 || translateY != 0 || translateZ != 0) {
								meshes[meshIndex]!!.translate(translateX, translateY, translateZ)
							}
						}
					}
				}
				@Suppress("UNCHECKED_CAST")
				val combinedMeshes = Mesh(meshes as Array<Mesh?>, meshes.size)
				adjustmentFunctionMask = adjustmentFunctionMask or ModelFunctionMask.RECOLOR
				cachedModel = toolkit.createModel(combinedMeshes, adjustmentFunctionMask, preferenceFlags, 64, 850)
				for (recolorIndex in 0 until 10) {
					for (colorIndex in recolorPaletteSrc!![recolorIndex].indices) {
						if (this.recolorDstIndices!![recolorIndex] < recolorPaletteDst!![recolorIndex][colorIndex].size) {
							cachedModel!!.recolor(recolorPaletteSrc!![recolorIndex][colorIndex], recolorPaletteDst!![recolorIndex][colorIndex][this.recolorDstIndices!![recolorIndex]])
						}
					}
				}

				if (updateSecondaryAppearanceMask) {
					cachedModel!!.applyShading(displacementFunctionMask)
					synchronized(bodyModelCache) {
						bodyModelCache.put(cachedModel!!, PlayerAppearanceHash)
					}
					this.secondaryAppearanceHash = PlayerAppearanceHash
				}
			}
		}
		val copiedModel = cachedModel!!.copy(4.toByte(), displacementFunctionMask, true)!!
		if (!functionMaskChanged && !requiresRotation) {
			return copiedModel
		}
		var submeshMatrices: Array<Matrix43?>? = null
		if (basType != null) {
			submeshMatrices = basType.getSubmeshMatrices()
		}
		if (requiresRotation && null != submeshMatrices) {
			for (index in rotations!!.indices) {
				if (submeshMatrices[index] != null) {
					copiedModel.offsetSubmesh(submeshMatrices[index]!!, 1 shl index, true)
				}
			}
		}
		var seqIndex = 0
		var submesh = 1
		while (seqIndex < objSequenceCount) {
			if (null != objSequences!![seqIndex]) {
				objSequences[seqIndex]!!.animateUnrotatedModel(copiedModel, submesh)
			}
			seqIndex++
			submesh = submesh shl 1
		}
		if (requiresRotation) {
			for (rotationIndex in rotations!!.indices) {
				if (-1 != rotations[rotationIndex]) {
					var rotation = rotations[rotationIndex] - rotationDelta
					rotation = rotation and 0x3fff
					val matrix = Matrix43()
					matrix.rotateMatrix(0.0f, 1.0f, 0.0f, MathUtils.getTheta(rotation))
					copiedModel.offsetSubmesh(matrix, 1 shl rotationIndex, false)
				}
			}
		}
		if (requiresRotation && submeshMatrices != null) {
			for (matriceIndex in rotations!!.indices) {
				if (submeshMatrices[matriceIndex] != null) {
					copiedModel.offsetSubmesh(submeshMatrices[matriceIndex]!!, 1 shl matriceIndex, false)
				}
			}
		}
		if (animatingSequence != null && null != idleSequence) {
			Sequence.doubleAnimation(copiedModel!!, animatingSequence, idleSequence)
		} else if (null != animatingSequence) {
			animatingSequence.animateRotatedModel(copiedModel!!, 0)
		} else if (null != idleSequence) {
			idleSequence.animateRotatedModel(copiedModel!!, 0)
		}
		return copiedModel
	}

	fun getBodyModel(toolkit: AbstractToolkit, initialFunctionMask: Int, basTypeList: BasTypeList, idkTypeList: IdkTypeList, loader: NpcTypeList, objTypeList: ItemTypeList, varProvider: VarDomain, animatingSequence: Sequence?, idleSequence: Sequence?, objSequence: Array<Sequence?>?, rotations: IntArray?, rotationDelta: Int, equipmentDefaults: EquipmentDefaultsType, privatePool: PoseCopyPair? = null): Model? {
		if (this.morphism != -1) {
			return loader.list(this.morphism).getAnimatedBodymodel(toolkit, initialFunctionMask, basTypeList, varProvider, animatingSequence, idleSequence, objSequence, rotations, rotationDelta, null, privatePool = privatePool)
		}
		var displacementFunctionMask = initialFunctionMask
		var AppearanceHash = this.playerAppearanceHash
		var playerAppearance = this.appearance
		var hasReplacementShield = false
		var hasReplacementWeapon = false
		if (animatingSequence != null) {
			val seqType = animatingSequence.getType()
			if (seqType != null && (seqType.replacementShield >= 0 || seqType.replacementWeapon >= 0)) {
				playerAppearance = IntArray(this.appearance!!.size)
				System.arraycopy(this.appearance!!, 0, playerAppearance, 0, playerAppearance.size)
				if (seqType.replacementShield >= 0 && equipmentDefaults.shieldSlot != -1) {
					hasReplacementShield = true
					if (seqType.replacementShield == 65535) {
						playerAppearance[equipmentDefaults.shieldSlot] = 0
						for (index in 0 until equipmentDefaults.hiddenAnimationShieldSlots!!.size) {
							playerAppearance[equipmentDefaults.hiddenAnimationShieldSlots!![index]] = 0
						}
						AppearanceHash = AppearanceHash xor -4294967296L
					} else {
						playerAppearance[equipmentDefaults.shieldSlot] = seqType.replacementShield or 0x40000000
						for (index in 0 until equipmentDefaults.hiddenAnimationShieldSlots!!.size) {
							playerAppearance[equipmentDefaults.hiddenAnimationShieldSlots!![index]] = 0
						}
						AppearanceHash = AppearanceHash xor (playerAppearance[equipmentDefaults.shieldSlot].toLong() shl 32)
					}
				}
				if (seqType.replacementWeapon >= 0 && equipmentDefaults.weaponSlot != -1) {
					hasReplacementWeapon = true
					if (seqType.replacementWeapon == 65535) {
						playerAppearance[equipmentDefaults.weaponSlot] = 0
						for (index in 0 until equipmentDefaults.hiddenAnimationWeaponSlots!!.size) {
							playerAppearance[equipmentDefaults.hiddenAnimationWeaponSlots!![index]] = 0
						}
						AppearanceHash = AppearanceHash xor 0xffffffffL
					} else {
						playerAppearance[equipmentDefaults.weaponSlot] = seqType.replacementWeapon or 0x40000000
						for (index in 0 until equipmentDefaults.hiddenAnimationWeaponSlots!!.size) {
							playerAppearance[equipmentDefaults.hiddenAnimationWeaponSlots!![index]] = 0
						}
						AppearanceHash = AppearanceHash xor playerAppearance[equipmentDefaults.weaponSlot].toLong()
					}
				}
			}
		}
		var flagAdjustment = false
		val index = objSequence?.size ?: 0
		for (animationIndex in 0 until index) {
			if (objSequence!![animationIndex] != null) {
				displacementFunctionMask = displacementFunctionMask or objSequence[animationIndex]!!.getFunctionMask()
				flagAdjustment = true
			}
		}
		if (animatingSequence != null) {
			displacementFunctionMask = displacementFunctionMask or animatingSequence.getFunctionMask()
			flagAdjustment = true
		}
		if (idleSequence != null) {
			displacementFunctionMask = displacementFunctionMask or idleSequence.getFunctionMask()
			flagAdjustment = true
		}
		var requiresRotation = false
		if (rotations != null) {
			for (rotationSlot in 0 until rotations.size) {
				if (rotations[rotationSlot] != -1) {
					displacementFunctionMask = displacementFunctionMask or 0x20
					requiresRotation = true
				}
			}
		}
		var model: Model?
		synchronized(bodyModelCache) {
			model = bodyModelCache.get(AppearanceHash) as Model?
		}
		var basType: BasType? = null
		if (this.basId != -1) {
			basType = basTypeList.list(this.basId)
		}
		if (model == null || toolkit.mismatchingFunctionMask(model!!.getFunctionMask(), displacementFunctionMask) != 0) {
			if (model != null) {
				displacementFunctionMask = toolkit.mergeFunctionMasks(displacementFunctionMask, model.getFunctionMask())
			}
			var missingModels = false
			var currentSlot = 0
			while (true) {
				if (currentSlot >= playerAppearance!!.size) {
					if (missingModels) {
						if (this.bodyModelHash != -1L) {
							synchronized(bodyModelCache) {
								model = bodyModelCache.get(this.bodyModelHash) as Model?
							}
						}
						if (model == null || toolkit.mismatchingFunctionMask(model!!.getFunctionMask(), displacementFunctionMask) != 0) {
							return null
						}
					} else {
						val meshesToCombine = arrayOfNulls<Mesh?>(playerAppearance.size)
						for (slot in 0 until playerAppearance.size) {
							val bitpackedAppearancePart = playerAppearance[slot]
							var meshModifier: ItemTypeCustomisation? = null
							val objReplacement = slot == 5 && hasReplacementShield || slot == 3 && hasReplacementWeapon
							if ((bitpackedAppearancePart and 0x40000000) != 0) {
								if (!objReplacement && this.meshModifiers != null && this.meshModifiers!![slot] != null) {
									meshModifier = this.meshModifiers!![slot]
								}
								val objBodyMesh = objTypeList.list(bitpackedAppearancePart and 0x3fffffff).getBodyMesh(this.female, meshModifier)
								if (objBodyMesh != null) {
									meshesToCombine[slot] = objBodyMesh
								}
							} else if ((bitpackedAppearancePart and -2147483648) != 0) {
								val idkBodyMesh = idkTypeList.list(bitpackedAppearancePart and 0x3fffffff).getBodyMesh()
								if (idkBodyMesh != null) {
									meshesToCombine[slot] = idkBodyMesh
								}
							}
						}
						if (basType != null && basType.objVerticeTransformations != null) {
							for (matrixIndex in basType.objVerticeTransformations!!.indices) {
								if (meshesToCombine[matrixIndex] != null) {
									var translateX = 0
									var translateY = 0
									var translateZ = 0
									var rotateX = 0
									var rotateY = 0
									var rotateZ = 0
									if (basType.objVerticeTransformations!![matrixIndex] != null) {
										translateX = basType.objVerticeTransformations!![matrixIndex]!![0]
										translateY = basType.objVerticeTransformations!![matrixIndex]!![1]
										translateZ = basType.objVerticeTransformations!![matrixIndex]!![2]
										rotateX = basType.objVerticeTransformations!![matrixIndex]!![3] shl 3
										rotateY = basType.objVerticeTransformations!![matrixIndex]!![4] shl 3
										rotateZ = basType.objVerticeTransformations!![matrixIndex]!![5] shl 3
									}
									if (rotateX != 0 || rotateY != 0 || rotateZ != 0) {
										meshesToCombine[matrixIndex]!!.rotateWithoutNormals(rotateX, rotateY, rotateZ)
									}
									if (translateX != 0 || translateY != 0 || translateZ != 0) {
										meshesToCombine[matrixIndex]!!.translate(translateX, translateY, translateZ)
									}
								}
							}
						}
						@Suppress("UNCHECKED_CAST")
						val combinedMesh = Mesh(meshesToCombine as Array<Mesh?>, meshesToCombine.size)
						var adjustmentFunctionMask = displacementFunctionMask or 0x4000
						model = toolkit.createModel(combinedMesh, adjustmentFunctionMask, preferenceFlags, 64, 850)
						for (recolorIndex in 0 until 10) {
							for (colorIndex in recolorPaletteSrc!![recolorIndex].indices) {
								if (this.recolorDstIndices!![recolorIndex] < recolorPaletteDst!![recolorIndex][colorIndex].size) {
									model!!.recolor(recolorPaletteSrc!![recolorIndex][colorIndex], recolorPaletteDst!![recolorIndex][colorIndex][this.recolorDstIndices!![recolorIndex]])
								}
							}
						}
						model!!.applyShading(displacementFunctionMask)
						synchronized(bodyModelCache) {
							bodyModelCache.put(model!!, AppearanceHash)
						}
						this.bodyModelHash = AppearanceHash
					}
					break
				}
				val bitpackedAppearancePart = playerAppearance[currentSlot]
				var slotObjReplaced = false
				if (hasReplacementShield) {
					if (equipmentDefaults.shieldSlot == currentSlot) {
						slotObjReplaced = true
					} else {
						for (slotIndex in 0 until equipmentDefaults.hiddenAnimationShieldSlots!!.size) {
							if (equipmentDefaults.hiddenAnimationShieldSlots!![slotIndex] == currentSlot) {
								slotObjReplaced = true
								break
							}
						}
					}
				}
				if (hasReplacementWeapon) {
					if (equipmentDefaults.weaponSlot == currentSlot) {
						slotObjReplaced = true
					} else {
						for (slotIndex in 0 until equipmentDefaults.hiddenAnimationWeaponSlots!!.size) {
							if (equipmentDefaults.hiddenAnimationWeaponSlots!![slotIndex] == currentSlot) {
								slotObjReplaced = true
								break
							}
						}
					}
				}
				if ((bitpackedAppearancePart and 0x40000000) != 0) {
					var meshModifier: ItemTypeCustomisation? = null
					if (!slotObjReplaced && this.meshModifiers != null && this.meshModifiers!![currentSlot] != null) {
						meshModifier = this.meshModifiers!![currentSlot]
					}
					if (!objTypeList.list(bitpackedAppearancePart and 0x3fffffff).isBodyCached(this.female, meshModifier)) {
						missingModels = true
					}
				} else if ((bitpackedAppearancePart and -2147483648) != 0 && !idkTypeList.list(bitpackedAppearancePart and 0x3fffffff).isBodyCached()) {
					missingModels = true
				}
				currentSlot++
			}
		}
		val copiedModel: Model = if (privatePool != null && model is ModelGL) privatePool.copy(model as ModelGL, displacementFunctionMask) else model!!.copy(4.toByte(), displacementFunctionMask, true)!!
		if (privatePool != null && copiedModel is ModelGL && model is ModelGL) copiedModel.detachSharedNormals(model as ModelGL)
		if (flagAdjustment || requiresRotation) {
			var submeshMatrices: Array<Matrix43?>? = null
			if (basType != null) {
				submeshMatrices = basType.getSubmeshMatrices()
			}
			if (requiresRotation && submeshMatrices != null) {
				for (submeshIndex in rotations!!.indices) {
					if (submeshMatrices[submeshIndex] != null) {
						copiedModel.offsetSubmesh(submeshMatrices[submeshIndex]!!, 1 shl submeshIndex, true)
					}
				}
			}
			var sequenceIndex = 0
			var submesh = 1
			while (sequenceIndex < index) {
				if (objSequence!![sequenceIndex] != null) {
					objSequence[sequenceIndex]!!.animateUnrotatedModel(copiedModel, submesh)
				}
				sequenceIndex++
				submesh = submesh shl 1
			}
			if (requiresRotation) {
				for (rotationIndex in rotations!!.indices) {
					if (rotations[rotationIndex] != -1) {
						var rotation = rotations[rotationIndex] - rotationDelta
						rotation = rotation and 0x3fff
						val rotatedMatrix = Matrix43()
						rotatedMatrix.rotateMatrix(0.0f, 1.0f, 0.0f, MathUtils.getTheta(rotation))
						copiedModel.offsetSubmesh(rotatedMatrix, 1 shl rotationIndex, false)
					}
				}
			}
			if (requiresRotation && submeshMatrices != null) {
				for (matrixIndex in rotations!!.indices) {
					if (submeshMatrices[matrixIndex] != null) {
						copiedModel.offsetSubmesh(submeshMatrices[matrixIndex]!!, 1 shl matrixIndex, false)
					}
				}
			}
			if (animatingSequence != null && idleSequence != null) {
				Sequence.doubleAnimation(copiedModel!!, animatingSequence, idleSequence)
			} else if (animatingSequence != null) {
				animatingSequence.animateRotatedModel(copiedModel!!, 0)
			} else if (idleSequence != null) {
				idleSequence.animateRotatedModel(copiedModel!!, 0)
			}
		}
		return copiedModel
	}

	fun getPlainAnimatedHeadModel(toolkit: AbstractToolkit, initialFunctionMask: Int, identityTypeList: IdkTypeList, sequence: Sequence?, primaryHead: Int, secondaryHead: Int, tertiaryHead: Int): Model? {
		val displacementFunctionMask = if (sequence != null) initialFunctionMask or sequence.getFunctionMask() else initialFunctionMask
		val headUid = (tertiaryHead.toLong() shl 32) or ((secondaryHead.toLong() shl 16) or primaryHead.toLong())
		var model: Model?
		synchronized(headModelCache) {
			model = headModelCache.get(headUid) as Model?
		}
		var adjustedDisplacementFunctionMask = displacementFunctionMask
		if (model == null || toolkit.mismatchingFunctionMask(model!!.getFunctionMask(), adjustedDisplacementFunctionMask) != 0) {
			if (model != null) {
				adjustedDisplacementFunctionMask = toolkit.mergeFunctionMasks(adjustedDisplacementFunctionMask, model.getFunctionMask())
			}
			val headMeshes = arrayOfNulls<Mesh?>(3)
			var headParts = 0
			if (!identityTypeList.list(primaryHead).isHeadCached() || !identityTypeList.list(secondaryHead).isHeadCached() || !identityTypeList.list(tertiaryHead).isHeadCached()) {
				return null
			}
			val primaryHeadMesh = identityTypeList.list(primaryHead).getHeadMesh()
			if (primaryHeadMesh != null) {
				headMeshes[headParts++] = primaryHeadMesh
			}
			val secondaryHeadMesh = identityTypeList.list(secondaryHead).getHeadMesh()
			if (secondaryHeadMesh != null) {
				headMeshes[headParts++] = secondaryHeadMesh
			}
			val tertiaryHeadMesh = identityTypeList.list(tertiaryHead).getHeadMesh()
			if (tertiaryHeadMesh != null) {
				headMeshes[headParts++] = tertiaryHeadMesh
			}
			@Suppress("UNCHECKED_CAST")
			val combinedHeadMesh = Mesh(headMeshes as Array<Mesh?>, headParts)
			val adjustmentFunctionMask = adjustedDisplacementFunctionMask or ModelFunctionMask.RECOLOR
			model = toolkit.createModel(combinedHeadMesh, adjustmentFunctionMask, preferenceFlags, 64, 768)
			for (recolorIndex in 0 until 10) {
				for (colorIndex in recolorPaletteSrc!![recolorIndex].indices) {
					if (this.recolorDstIndices!![recolorIndex] < recolorPaletteDst!![recolorIndex][colorIndex].size) {
						model!!.recolor(recolorPaletteSrc!![recolorIndex][colorIndex], recolorPaletteDst!![recolorIndex][colorIndex][this.recolorDstIndices!![recolorIndex]])
					}
				}
			}
			model!!.applyShading(adjustedDisplacementFunctionMask)
			synchronized(headModelCache) {
				headModelCache.put(model!!, headUid)
			}
		}
		if (sequence != null) {
			model = model!!.copy(4.toByte(), adjustedDisplacementFunctionMask, true)
			sequence.animateRotatedModel(model!!, 0)
		}
		return model
	}

	fun getAnimatedHeadModel(toolkit: AbstractToolkit, initialFunctionMask: Int, idkTypeList: IdkTypeList, npcTypeList: NpcTypeList, objTypeList: ItemTypeList, varProvider: VarDomain, sequence: Sequence?): Model? {
		if (this.morphism != -1) {
			return npcTypeList.list(this.morphism).getAnimatedHeadmodel(toolkit, initialFunctionMask, varProvider, sequence, null)
		}
		val displacementFunctionMask = if (sequence != null) initialFunctionMask or sequence.getFunctionMask() else initialFunctionMask
		var model: Model?
		synchronized(headModelCache) {
			model = headModelCache.get(this.playerAppearanceHash) as Model?
		}
		var adjustedDisplacementFunctionMask = displacementFunctionMask
		if (model == null || toolkit.mismatchingFunctionMask(model!!.getFunctionMask(), adjustedDisplacementFunctionMask) != 0) {
			if (model != null) {
				adjustedDisplacementFunctionMask = toolkit.mergeFunctionMasks(adjustedDisplacementFunctionMask, model.getFunctionMask())
			}
			var missingModels = false
			for (index in this.appearance!!.indices) {
				val appearancePart = this.appearance!![index]
				var meshModifier: ItemTypeCustomisation? = null
				if ((appearancePart and 0x40000000) != 0) {
					if (this.meshModifiers != null && this.meshModifiers!![index] != null) {
						meshModifier = this.meshModifiers!![index]
					}
					if (!objTypeList.list(appearancePart and 0x3fffffff).isHeadCached(this.female, meshModifier)) {
						missingModels = true
					}
				} else if ((appearancePart and -2147483648) != 0 && !idkTypeList.list(appearancePart and 0x3fffffff).isHeadCached()) {
					missingModels = true
				}
			}
			if (missingModels) {
				return null
			}
			val meshesToCombine = arrayOfNulls<Mesh?>(this.appearance!!.size)
			var meshCount = 0
			for (index in this.appearance!!.indices) {
				val appearancePart = this.appearance!![index]
				if ((appearancePart and 0x40000000) != 0) {
					var meshModifier: ItemTypeCustomisation? = null
					if (this.meshModifiers != null && this.meshModifiers!![index] != null) {
						meshModifier = this.meshModifiers!![index]
					}
					val objMesh = objTypeList.list(appearancePart and 0x3fffffff).getHeadMesh(this.female, meshModifier)
					if (objMesh != null) {
						meshesToCombine[meshCount++] = objMesh
					}
				} else if ((appearancePart and -2147483648) != 0) {
					val idkMesh = idkTypeList.list(appearancePart and 0x3fffffff).getHeadMesh()
					if (idkMesh != null) {
						meshesToCombine[meshCount++] = idkMesh
					}
				}
			}
			@Suppress("UNCHECKED_CAST")
			val headMesh = Mesh(meshesToCombine as Array<Mesh?>, meshCount)
			val adjustmentFunctionMask = adjustedDisplacementFunctionMask or ModelFunctionMask.RECOLOR
			model = toolkit.createModel(headMesh, adjustmentFunctionMask, preferenceFlags, 64, 768)
			for (recolorIndex in 0 until 10) {
				for (colorIndex in recolorPaletteSrc!![recolorIndex].indices) {
					if (this.recolorDstIndices!![recolorIndex] < recolorPaletteDst!![recolorIndex][colorIndex].size) {
						model!!.recolor(recolorPaletteSrc!![recolorIndex][colorIndex], recolorPaletteDst!![recolorIndex][colorIndex][this.recolorDstIndices!![recolorIndex]])
					}
				}
			}
			model!!.applyShading(adjustedDisplacementFunctionMask)
			synchronized(headModelCache) {
				headModelCache.put(model!!, this.playerAppearanceHash)
			}
		}
		if (sequence == null) {
			return model
		}
		val animatedModel = model!!.copy(4.toByte(), adjustedDisplacementFunctionMask, true)!!
		sequence.animateRotatedModel(animatedModel, 0)
		return animatedModel
	}

	fun calculateAppearanceHash() {
		val crc64 = Hashing.CRC64_TABLE
		this.playerAppearanceHash = -1L
		this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor (this.basId shr 8).toLong()) and 0xffL).toInt()]
		this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor this.basId.toLong()) and 0xffL).toInt()]
		for (part in this.appearance!!.indices) {
			this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor (this.appearance!![part] shr 24).toLong()) and 0xffL).toInt()]
			this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor (this.appearance!![part] shr 16).toLong()) and 0xffL).toInt()]
			this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor (this.appearance!![part] shr 8).toLong()) and 0xffL).toInt()]
			this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor this.appearance!![part].toLong()) and 0xffL).toInt()]
		}
		if (this.meshModifiers != null) {
			for (index in this.meshModifiers!!.indices) {
				if (this.meshModifiers!![index] != null) {
					val body: IntArray?
					val head: IntArray?
					if (this.female) {
						body = this.meshModifiers!![index]!!.femaleBody
						head = this.meshModifiers!![index]!!.femaleHead
					} else {
						body = this.meshModifiers!![index]!!.maleBody
						head = this.meshModifiers!![index]!!.maleHead
					}
					if (body != null) {
						for (part in body.indices) {
							this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor (body[part] shr 8).toLong()) and 0xffL).toInt()]
							this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor body[part].toLong()) and 0xffL).toInt()]
						}
					}
					if (head != null) {
						for (part in head.indices) {
							this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor (head[part] shr 8).toLong()) and 0xffL).toInt()]
							this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor head[part].toLong()) and 0xffL).toInt()]
						}
					}
					if (this.meshModifiers!![index]!!.recolDst != null) {
						val recolDst = this.meshModifiers!![index]!!.recolDst!!
						for (part in recolDst.indices) {
							val recolValue = recolDst[part]
							val sv = recolValue.toInt() shr 8
							val idx1 = ((this.playerAppearanceHash xor sv.toLong()) and 0xffL).toInt()
							val hash1 = crc64[idx1]
							this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor hash1
							val idx2 = ((this.playerAppearanceHash xor recolValue.toLong()) and 0xffL).toInt()
							val hash2 = crc64[idx2]
							this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor hash2
						}
					}
					if (this.meshModifiers!![index]!!.retexDst != null) {
						val retexDst = this.meshModifiers!![index]!!.retexDst!!
						for (part in retexDst.indices) {
							val retexValue = retexDst[part]
							val sv = retexValue.toInt() shr 8
							val idx1 = ((this.playerAppearanceHash xor sv.toLong()) and 0xffL).toInt()
							val hash1 = crc64[idx1]
							this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor hash1
							val idx2 = ((this.playerAppearanceHash xor retexValue.toLong()) and 0xffL).toInt()
							val hash2 = crc64[idx2]
							this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor hash2
						}
					}
				}
			}
		}
		for (part in 0 until 10) {
			this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor this.recolorDstIndices!![part].toLong()) and 0xffL).toInt()]
		}
		this.playerAppearanceHash = this.playerAppearanceHash ushr 8 xor crc64[((this.playerAppearanceHash xor (if (this.female) 1 else 0).toLong()) and 0xffL).toInt()]
	}

	companion object {
		@JvmField
		val IDK_PART_TABLE = intArrayOf(8, 11, 4, 6, 9, 7, 10, 0)

		@JvmField
		val bodyModelCache = LRUCache(260)
		@JvmField
		val headModelCache = LRUCache(5)

		@JvmField
		var recolorPaletteSrc: Array<ShortArray>? = null
		@JvmField
		var recolorPaletteDst: Array<Array<ShortArray>>? = null

		@JvmField
		var preferenceFlags: Int = 0

		@JvmStatic
		fun processCacheCycle() {
			synchronized(bodyModelCache) {
				bodyModelCache.clean(5)
			}
			synchronized(headModelCache) {
				headModelCache.clean(5)
			}
		}

		@JvmStatic
		fun countBodyModels(): Int {
			synchronized(bodyModelCache) {
				val count = bodyModelCache.count()
				return count
			}
		}

		@JvmStatic
		fun resetCache() {
			synchronized(bodyModelCache) {
				bodyModelCache.reset()
			}
			synchronized(headModelCache) {
				headModelCache.reset()
			}
		}

		@JvmStatic
		fun clearQueue() {
			synchronized(bodyModelCache) {
				bodyModelCache.resize()
			}
			synchronized(headModelCache) {
				headModelCache.resize()
			}
		}

		@JvmStatic
		fun setPreferences(prefFlags: Int) {
			preferenceFlags = prefFlags
			synchronized(headModelCache) {
				headModelCache.reset()
			}
			synchronized(bodyModelCache) {
				bodyModelCache.reset()
			}
		}
	}
}
