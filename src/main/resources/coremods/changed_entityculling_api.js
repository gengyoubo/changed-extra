// Mixin requests transformed bytecode from ModLauncher before preprocessing it.
// A normal injection into WaveVisionRenderer would run too late to fix this link.
// Changed's mixin was built against an older EntityCulling: Cullable lived in versionless.access and
// NMSCullingHelper in the root package. Both links go through CullableBridge, which finds the
// installed API by reflection, so neither EntityCulling layout is linked directly.
function initializeCoreMod() {
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var oldType = 'dev/tr7zw/entityculling/versionless/access/Cullable';
    var oldHelper = 'dev/tr7zw/entityculling/NMSCullingHelper';
    var bridge = 'github/com/gengyoubo/CE/compat/entityculling/CullableBridge';

    function replaceFrameTypes(types) {
        if (types == null) return;
        for (var i = 0; i < types.size(); i++) {
            if (types.get(i) == oldType) types.set(i, 'java/lang/Object');
        }
    }

    function isCullableCall(insn) {
        return insn.getOpcode() == Opcodes.INVOKEINTERFACE &&
            ((insn.name == 'isForcedVisible' || insn.name == 'isCulled') && insn.desc == '()Z' ||
             insn.name == 'setOutOfCamera' && insn.desc == '(Z)V');
    }

    function isHelperCall(insn) {
        return insn.getOpcode() == Opcodes.INVOKESTATIC &&
            (insn.name == 'ignoresCulling' && insn.desc == '(Lnet/minecraft/world/entity/Entity;)Z' ||
             insn.name == 'getRenderOffset' && insn.desc ==
                '(Lnet/minecraft/client/renderer/entity/EntityRenderer;Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/world/phys/Vec3;');
    }

    return {
        'changed_wavevision_cullable': {
            'target': {
                'type': 'CLASS',
                'name': 'net.ltxprogrammer.changed.mixin.compatibility.EntityCulling.WaveVisionRendererMixin'
            },
            'transformer': function (node) {
                var calls = 0;
                // Leave newer Changed implementations alone. Refuse partial rewrites
                // if Changed starts using an additional legacy method.
                for (var m = 0; m < node.methods.size(); m++) {
                    var instructions = node.methods.get(m).instructions;
                    for (var i = 0; i < instructions.size(); i++) {
                        var insn = instructions.get(i);
                        if (!(insn instanceof MethodInsnNode)) continue;
                        if (insn.owner == oldType) {
                            if (!isCullableCall(insn)) throw new Error('Unsupported Changed Cullable call: ' + insn.name + insn.desc);
                            calls++;
                        } else if (insn.owner == oldHelper) {
                            if (!isHelperCall(insn)) throw new Error('Unsupported Changed NMSCullingHelper call: ' + insn.name + insn.desc);
                            calls++;
                        }
                    }
                }
                if (calls == 0) return node;

                for (var m = 0; m < node.methods.size(); m++) {
                    var method = node.methods.get(m);
                    for (var i = 0; i < method.instructions.size(); i++) {
                        var insn = method.instructions.get(i);
                        if (insn instanceof TypeInsnNode && insn.desc == oldType) {
                            insn.desc = 'java/lang/Object';
                        } else if (insn instanceof MethodInsnNode && insn.owner == oldType) {
                            insn.setOpcode(Opcodes.INVOKESTATIC);
                            insn.owner = bridge;
                            insn.desc = '(Ljava/lang/Object;' + insn.desc.substring(1);
                            insn.itf = false;
                        } else if (insn instanceof MethodInsnNode && insn.owner == oldHelper) {
                            // Same static signature on the bridge; only the owner changes.
                            insn.owner = bridge;
                            insn.itf = false;
                        } else if (insn instanceof FrameNode) {
                            replaceFrameTypes(insn.local);
                            replaceFrameTypes(insn.stack);
                        }
                    }
                    if (method.localVariables != null) {
                        for (var i = 0; i < method.localVariables.size(); i++) {
                            var local = method.localVariables.get(i);
                            if (local.desc == 'L' + oldType + ';') {
                                local.desc = 'Ljava/lang/Object;';
                                local.signature = null;
                            }
                        }
                    }
                }
                ASMAPI.log('INFO', 'Changed Extra bridged Changed WaveVisionRenderer to the installed EntityCulling API');
                return node;
            }
        }
    };
}
