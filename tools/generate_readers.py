import hashlib,json,re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
books=[
 ('semilla','La semilla de Luna','es','plant',['Luna encuentra una semilla.','La planta en una maceta con tierra.','La cuida con agua y luz. Una hoja comienza a crecer.'],'¿Qué cuidó Luna?',['Una semilla','Un zapato','Un reloj'],0),
 ('puente','El puente de papel','es','bridge',['Ana y Luis quieren hacer un puente.','Prueban una hoja plana. El puente se dobla.','Doblan otra hoja como un acordeón. Ahora el puente sostiene su ficha.'],'¿Qué cambiaron Ana y Luis?',['La forma del papel','El nombre de Luis','El día de la semana'],0),
 ('libro','Un libro para dos','es','book',['Sol quiere leer un cuento. Nico también.','Los dos conversan y buscan un lugar tranquilo.','Sol lee una página y Nico lee la siguiente. Comparten la historia.'],'¿Cómo compartieron la lectura?',['Leyendo por turnos','Rompiendo el libro','Escondiendo las páginas'],0),
 ('short-a','Inglés: palabras con a corta','en','cat',['A cat.','A cat on a mat.','A bat and a cat.'],'¿Qué palabra rima con CAT?',['MAT','SUN','PEN'],0),
 ('short-e','Inglés: palabras con e corta','en','book',['A hen.','A hen and a pen.','Ten hens.'],'¿Qué palabra rima con HEN?',['PEN','CAT','BIG'],0),
 ('short-i','Inglés: palabras con i corta','en','plant',['A pig.','A big pig.','A pig can dig.'],'¿Qué palabra rima con PIG?',['DIG','HEN','MAT'],0),
]
audio={};result=[]
for id,title,lang,scene,texts,question,choices,correct in books:
    pages=[]
    for n,text in enumerate(texts):
        key=f'read-page-{id}-{n}';audio[key]={'text':text,'language':lang}
        words=[]
        for word in re.findall(r'[^\W\d_]+',text,re.UNICODE):
            token=word.lower();wk='read-'+lang+'-'+hashlib.sha256(token.encode()).hexdigest()[:12]
            audio[wk]={'text':token,'language':lang};words.append({'text':word,'audio':wk})
        pages.append({'text':text,'audio':key,'words':words})
    result.append({'id':id,'title':title,'language':lang,'scene':scene,'pages':pages,'question':question,'choices':choices,'correct':correct})
(root/'app/src/main/assets/content/readers-v1.json').write_text(json.dumps({'books':result},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(root/'tools/reader_audio.json').write_text(json.dumps(audio,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'{len(result)} books, {len(audio)} local recordings')
