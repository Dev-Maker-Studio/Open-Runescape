import re,os,sys,collections
root='/data/projects/Personal/OpenRS-Web/assets/cache/clientscripts'
decl={}
txt=open(root+'/cs2.d.ts').read().split('\n')
for i,l in enumerate(txt):
    m=re.match(r'/\*\* `([A-Z0-9_]+)` \(opcode',l)
    if m:
        n=re.match(r'declare function (\w+)',txt[i+1])
        if n: decl[n.group(1)]=m.group(1)
sup=set(l.strip() for l in open(sys.argv[1]))
files={}
for d in ('cs2','proc','clientscript'):
    for f in os.listdir(f'{root}/{d}'):
        if f.endswith('.ts'): files[f[:-3]]=open(f'{root}/{d}/{f}').read()
call=re.compile(r'\b(\w+)\s*(?:<[^>()]*>)?\(')
direct={};calls={}
for n,t in files.items():
    ids=set(call.findall(t))
    direct[n]={decl[i] for i in ids if i in decl}
    calls[n]={i for i in ids if i in files and i!=n}
memo={}
def reach(n,stack=()):
    if n in memo: return memo[n]
    s=set(direct[n]);memo[n]=s
    for c in calls[n]:
        if c not in stack: s|=reach(c,stack+(n,))
    return s
cnt=collections.Counter();dcnt=collections.Counter()
for n in files:
    r=reach(n)
    for o in r:
        if o not in sup: cnt[o]+=1
    for o in direct[n]:
        if o not in sup: dcnt[o]+=1
print('op\treaching_scripts\tdirect_scripts')
for o,c in cnt.most_common(): print(f'{o}\t{c}\t{dcnt[o]}')
print(len(files),file=sys.stderr)
