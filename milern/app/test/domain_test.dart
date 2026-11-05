import 'package:flutter_test/flutter_test.dart';
import 'package:milern/models/learning_node.dart';
import 'package:milern/services/document_scanner.dart';
import 'package:milern/services/reminders.dart';
import 'package:timezone/data/latest_all.dart' as data;
import 'package:timezone/timezone.dart' as tz;

void main() {
  const node = LearningNode(
    prompt: '¿Pregunta?',
    answer: 'Respuesta',
    minuteOfDay: 540,
  );
  test('silencio y campos obligatorios', () {
    expect(validateNode(node.copyWith(prompt: '  '), []), isNotNull);
    expect(validateNode(node.copyWith(answer: '  '), []), isNotNull);
    for (final minute in [0, 419, 1320, 1439]) {
      expect(validateNode(node.copyWith(minuteOfDay: minute), []), isNotNull);
    }
    for (final minute in [420, 1319]) {
      expect(validateNode(node.copyWith(minuteOfDay: minute), []), isNull);
    }
  });
  test('un esfuerzo por minuto y editar el mismo nodo', () {
    expect(validateNode(node, [node.copyWith(id: 1)]), isNotNull);
    expect(validateNode(node.copyWith(id: 1), [node.copyWith(id: 1)]), isNull);
    expect(validateNode(node.copyWith(enabled: false), [node]), isNull);
    expect(availableMinute([node]), 541);
  });
  test('limite de solicitudes activas sin limitar nodos pausados', () {
    final full = List.generate(
      60,
      (i) => node.copyWith(id: i + 1, minuteOfDay: 600 + i),
    );
    expect(validateNode(node, full), contains('60'));
    expect(validateNode(node.copyWith(enabled: false), full), isNull);
    expect(validateNode(full.first.copyWith(prompt: 'Editado'), full), isNull);
  });
  test('mock reproducible, sin leer imagen y con horarios libres', () {
    final generated = mockDocumentNodes([node]);
    expect(generated.length, 13);
    expect(generated.map((n) => n.prompt).toList(), [
      'Zoología',
      'Botánica',
      'Micología',
      'Paleontología',
      'Ecología',
      'Taxonomía',
      'Sistemática',
      'Embriología',
      'Fisiología',
      'Citología',
      '¿Por qué se divide la biología en ramas?',
      'Aplicaciones médicas clave',
      'Aplicaciones en alimentación',
    ]);
    expect(
      generated.first.answer,
      'Animales: funciones vitales y formas de vida.',
    );
    expect(
      generated.last.answer,
      'Aumento de la producción mundial de alimentos y hormonas para floración.',
    );
    expect(
      generated.map((n) => n.minuteOfDay).toList(),
      List.generate(13, (i) => 541 + i),
    );
    expect(generated.every((n) => n.source == 'Escaneo'), isTrue);
  });
  test('siguiente aviso hoy o mañana en hora local', () {
    data.initializeTimeZones();
    final lima = tz.getLocation('America/Lima');
    expect(
      nextReminder(tz.TZDateTime(lima, 2026, 11, 5, 8), 540),
      tz.TZDateTime(lima, 2026, 11, 5, 9),
    );
    expect(
      nextReminder(tz.TZDateTime(lima, 2026, 11, 5, 9), 540),
      tz.TZDateTime(lima, 2026, 11, 6, 9),
    );
    final ny = tz.getLocation('America/New_York');
    // Calendar-based next day, not a fixed 24h increment over DST.
    expect(
      nextReminder(tz.TZDateTime(ny, 2026, 3, 7, 12), 540),
      tz.TZDateTime(ny, 2026, 3, 8, 9),
    );
  });
}
