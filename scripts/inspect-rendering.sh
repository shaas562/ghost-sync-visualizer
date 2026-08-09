#!/bin/sh
set -eu

CACHE_ROOT="${GRADLE_USER_HOME:-/home/gradle/.gradle}/caches"

echo '=== Rendering classes ==='
find "$CACHE_ROOT" -type f -name '*.jar' 2>/dev/null | while IFS= read -r jar_file; do
    matches=$(jar tf "$jar_file" 2>/dev/null | grep -E '/(BlockRenderDispatcher|ModelBlockRenderer|SectionCompiler|SectionRenderDispatcher|BlockStateModel|SubmitNodeCollector|ChunkSectionLayer|BlockRenderLayerMap)\.class$' || true)
    if [ -n "$matches" ]; then
        printf '\n=== %s ===\n%s\n' "$jar_file" "$matches"
    fi
done

find_class_jar() {
    class_path="$1"
    find "$CACHE_ROOT" -type f -name '*.jar' 2>/dev/null | while IFS= read -r jar_file; do
        if jar tf "$jar_file" 2>/dev/null | grep -q "^${class_path}\.class$"; then
            printf '%s\n' "$jar_file"
            break
        fi
    done
}

print_sig() {
    class_name="$1"
    class_path=$(printf '%s' "$class_name" | tr '.' '/')
    class_jar=$(find_class_jar "$class_path" || true)
    printf '\n--- %s ---\n' "$class_name"
    if [ -n "$class_jar" ]; then
        javap -classpath "$class_jar" -p "$class_name" 2>/dev/null || true
    else
        echo 'not found'
    fi
}

for class_name in \
    net.minecraft.client.renderer.block.BlockRenderDispatcher \
    net.minecraft.client.renderer.block.ModelBlockRenderer \
    net.minecraft.client.renderer.chunk.SectionCompiler \
    net.minecraft.client.renderer.chunk.SectionRenderDispatcher \
    net.minecraft.client.renderer.block.model.BlockStateModel \
    net.minecraft.client.renderer.SubmitNodeCollector \
    net.minecraft.client.renderer.chunk.ChunkSectionLayer \
    net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap
do
    print_sig "$class_name"
done
