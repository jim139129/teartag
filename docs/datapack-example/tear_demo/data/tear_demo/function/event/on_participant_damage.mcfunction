# Copy the synchronous event context before any scheduled function uses it.
data modify storage tear_demo:last_event current set from storage teartag:event {}
