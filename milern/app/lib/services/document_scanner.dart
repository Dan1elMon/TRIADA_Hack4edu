import 'package:flutter/material.dart';
import 'package:image_cropper/image_cropper.dart';
import 'package:image_picker/image_picker.dart';

import '../models/learning_node.dart';

class DocumentScanner {
  final picker = ImagePicker();
  Future<String?> capture(ImageSource source) async {
    final image = await picker.pickImage(
      source: source,
      maxWidth: 2400,
      imageQuality: 88,
    );
    if (image == null) return null;
    return crop(image.path);
  }

  Future<String?> recover() async {
    final response = await picker.retrieveLostData();
    if (response.exception != null) throw response.exception!;
    final images = response.files;
    return images == null || images.isEmpty ? null : images.first.path;
  }

  Future<String?> crop(String path) async {
    final result = await ImageCropper().cropImage(
      sourcePath: path,
      uiSettings: [
        AndroidUiSettings(
          toolbarTitle: 'Recortar documento',
          toolbarColor: const Color(0xFF182B28),
          toolbarWidgetColor: Colors.white,
          lockAspectRatio: false,
        ),
        IOSUiSettings(
          title: 'Recortar documento',
          doneButtonTitle: 'Usar',
          cancelButtonTitle: 'Cancelar',
        ),
      ],
    );
    return result?.path;
  }
}

/// Intentionally independent of image contents: no OCR, model or network request.
List<LearningNode> mockDocumentNodes(List<LearningNode> existing) {
  const cards = [
    ('Zoología', 'Animales: funciones vitales y formas de vida.'),
    ('Botánica', 'Plantas y sus interacciones con otros seres vivos.'),
    ('Micología', 'Hongos y su relación con el medio ambiente.'),
    ('Paleontología', 'Seres vivos de épocas remotas a partir de fósiles.'),
    ('Ecología', 'Interacción entre los organismos y el medio ambiente.'),
    ('Taxonomía', 'Clasificación y nomenclatura por caracteres comunes.'),
    (
      'Sistemática',
      'Biodiversidad según relaciones jerárquicas de linajes (evolución).',
    ),
    ('Embriología', 'Formación y desarrollo a partir del embrión.'),
    ('Fisiología', 'Procesos y funciones de partes, órganos y tejidos.'),
    ('Citología', 'La célula: estructura, desarrollo, reproducción y función.'),
    (
      '¿Por qué se divide la biología en ramas?',
      'Por la gran diversidad de conocimientos y objetos de estudio a organizar.',
    ),
    (
      'Aplicaciones médicas clave',
      'Trasplantes (riñón, hígado, corazón), tratamientos genéticos y células madre.',
    ),
    (
      'Aplicaciones en alimentación',
      'Aumento de la producción mundial de alimentos y hormonas para floración.',
    ),
  ];
  final allocated = [...existing];
  final result = <LearningNode>[];
  for (final (prompt, answer) in cards) {
    final node = LearningNode(
      prompt: prompt,
      answer: answer,
      minuteOfDay: availableMinute(allocated),
      source: 'Escaneo',
    );
    result.add(node);
    allocated.add(node);
  }
  return result;
}
