"""Structural and arithmetic checks. Does not certify pedagogical suitability."""
import csv,json,re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets/content'
catalog=json.loads((assets/'catalog-v3.json').read_text(encoding='utf-8'))
expanded=json.loads((assets/'expanded-lessons-v1.json').read_text(encoding='utf-8'))['lessons']
ids=set(); arithmetic=0
for lesson in expanded:
    assert len(lesson['questions'])==3
    prompts=set()
    for q in lesson['questions']:
        assert q['id'] not in ids;ids.add(q['id'])
        assert q['prompt'] not in prompts;prompts.add(q['prompt'])
        choices=q['payload']['choices'];assert len({c['text'] for c in choices})==len(choices)
        correct=[c['text'] for c in choices if c['id']==q['answerKey']['choice']];assert len(correct)==1
        assert len(q['hints'])>=2 and all(h.strip() for h in q['hints'])
        m=re.fullmatch(r'¿Cuánto es (\d+) ([+−×÷]) (\d+)\?',q['prompt'])
        if m:
            a,b=int(m[1]),int(m[3]);expected={'+':lambda:a+b,'−':lambda:a-b,'×':lambda:a*b,'÷':lambda:a/b}[m[2]]()
            assert float(correct[0])==expected,(q['id'],correct,expected);arithmetic+=1
output=root/'AulaNexo/docs/revision_pedagogica_214.csv'
with output.open('w',encoding='utf-8-sig',newline='') as f:
    writer=csv.writer(f);writer.writerow(['tema_id','titulo','preguntas','tipo_revision','revision_docente','observaciones','revisor','fecha'])
    for topic in catalog['topics']:
        advanced=bool(re.search(r'Multiplic|Dividir|3 Cifras|500|999|Centena|Estadística|Tercia',topic['title'],re.I))
        note='Revisar adecuación a la edad y secuencia antes de uso escolar' if advanced else 'Revisar claridad, distractores y apoyo requerido'
        if 'Proyecto' in topic['title']:note+='; complemento práctico con supervisión adulta'
        writer.writerow([topic['id'],topic['title'],3,'Contenido introductorio original','Pendiente',note,'',''])
print(json.dumps({'topics':len(catalog['topics']),'new_lessons':len(expanded),'new_questions':len(ids),'checked_arithmetic_items':arithmetic,'teacher_review':'pending'},ensure_ascii=False))
