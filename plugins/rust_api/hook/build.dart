import 'dart:io';

import 'package:code_assets/code_assets.dart';
import 'package:flutter_rust_bridge_hooks/flutter_rust_bridge_hooks.dart';

void main(List<String> args) async {
  await build(args, (input, output) async {
    if (input.userDefines['build_assets'] == false) {
      stdout.writeln('Skipping the Rust build: user-define build_assets=false');
      return;
    }
    await FlutterRustBridgeNativeAssetsBuilder(
      cratePath: 'rust',
      extraCargoEnvironmentVariables: _bindgenEnvironment(input),
    ).run(input: input, output: output);
  });
}

// rquickjs runs bindgen on Android, which must load the NDK's libclang; Linux
// NDKs before r26 keep it under lib64, later ones and every macOS NDK under lib.
Map<String, String> _bindgenEnvironment(BuildInput input) {
  if (!input.config.buildCodeAssets ||
      input.config.code.targetOS != OS.android) {
    return const {};
  }
  final compiler = input.config.code.cCompiler?.compiler;
  if (compiler == null) {
    return const {};
  }
  final llvmRoot = File.fromUri(compiler).parent.parent;
  for (final name in const ['lib', 'lib64']) {
    final directory = Directory(
      '${llvmRoot.path}${Platform.pathSeparator}$name',
    );
    if (directory.existsSync() && directory.listSync().any(_isLibclang)) {
      final environment = {'LIBCLANG_PATH': directory.path};
      // bindgen resolves the header search list by running this clang, and
      // only the NDK's own clang names the resource directory holding
      // stdbool.h. The libclang above is a bare parser, so pairing it with a
      // foreign clang silently drops that directory.
      final clang = _ndkClang(compiler);
      if (clang != null) {
        environment['CLANG_PATH'] = clang;
      }
      return environment;
    }
  }
  throw StateError(
    'No libclang under ${llvmRoot.path} (lib or lib64); the NDK Flutter '
    'passed cannot run bindgen for rquickjs',
  );
}

// The clang beside the compiler Flutter handed us is the one whose resource
// directory matches this NDK, so point bindgen back at it.
String? _ndkClang(Uri compiler) {
  final candidate = File.fromUri(compiler);
  if (candidate.existsSync()) return candidate.path;
  final windows = File('${candidate.path}.exe');
  return windows.existsSync() ? windows.path : null;
}

bool _isLibclang(FileSystemEntity entity) {
  return entity.path.split(Platform.pathSeparator).last.startsWith('libclang.');
}
