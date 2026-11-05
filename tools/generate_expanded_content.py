"""Build original introductory exercises; source rows are reviewable, not scraped worksheets."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets/content'
catalog=json.loads((ASSETS/'catalog-v2.json').read_text(encoding='utf-8-sig'))
topics={t['id']:t for t in catalog['topics']}
units={u['subjectId']:u['id'] for u in catalog['units']}
rows={}
for path in sorted((ROOT/'tools').glob('curriculum_*.tsv')):
    for line in path.read_text(encoding='utf-8-sig').splitlines():
        if not line.strip():continue
        parts=line.split('|')
        assert len(parts)==10,(path,parts[0],len(parts))
        key=parts[0];topic=f'g1-{key[:3]}-t{key[3:]}'
        assert topic in topics and topic not in rows,topic
        rows[topic]=parts[1:]
expected={t['id'] for t in topics.values() if t['contentStatus']=='CATALOG_ONLY'}
assert set(rows)==expected,(expected-set(rows),set(rows)-expected)
lessons=[]
for topic_id,parts in rows.items():
    topic=topics[topic_id];lesson_id=topic_id+'-intro'
    questions=[]
    for index in range(3):
        prompt,correct,wrong=parts[index*3:index*3+3]
        assert correct!=wrong
        choices=[{'id':'a','text':correct},{'id':'b','text':wrong}]
        if (topic['sourceOrder']+index)%2:choices.reverse()
        questions.append({'id':lesson_id+f'-q{index+1}','prompt':prompt,'objectCount':0,'type':'SINGLE_CHOICE',
          'payload':{'instructions':'Lee o pide a un adulto que lea la pregunta. Compara las dos opciones.',
             'choices':choices,'feedbackCorrect':'¡Bien! Has relacionado la pregunta con su respuesta.',
             'feedbackIncorrect':'Revisa las palabras de la pregunta y compara las opciones. Puedes usar una pista.'},
          'answerKey':{'choice':'a'},'options':[],
          'hints':['Vuelve a leer la pregunta e identifica de qué objeto, acción o cantidad habla.',
                   'Prueba cada opción dentro de la pregunta y descarta la que no corresponde.']})
    lessons.append({'id':lesson_id,'version':1,'title':topic['title'],'objective':'Explorar '+topic['title'].lower()+' con tres actividades introductorias originales.','questions':questions})
    topic['contentStatus']='AVAILABLE';topic['unitId']=topic['unitId'] or units[topic['subjectId']]
    catalog['lessons'].append({'lessonId':lesson_id,'contentVersion':1,'topicId':topic_id})
catalog['catalogVersion']=3
(ASSETS/'catalog-v3.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(ASSETS/'expanded-lessons-v1.json').write_text(json.dumps({'lessons':lessons},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'{len(lessons)} new lessons; {sum(len(l["questions"]) for l in lessons)} new questions; {len(catalog["lessons"])} total lesson links')
