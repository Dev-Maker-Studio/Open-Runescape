# Engine-developer memory index

- [Shared test account + burst captures](shared-test-account-and-burst-captures.md) — testes is shared across agent sessions (mutual kicks, moved character); tele in the same run; ifbutton 182:6 = in-JVM logout; N-frame bursts for flicker bugs
- [UI-over-animated-model flicker (#22)](ui-over-animated-model-flicker.md) — deferred pose builds were not "pending scene geometry"; fix = count dbCount; A/B with a reverted jar
- [Player bob after login (#27) not reproduced](player-bob-not-reproduced.md) — every CPU-side vertical input verified constant with -Drender.playerydiag; what was tried and what is left
- [Bindless material table lifecycle](bindless-material-table-lifecycle.md) — material textures never deleted (finalizer regression) masks a latent stale-handle hole at the retained walk; verified-OK list
