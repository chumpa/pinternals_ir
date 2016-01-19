attach database 'notes_old.db' as 'old';
.databases
insert into area(area,text,descr,parent) select area,text,descr,parent from old.area;
insert into main(number,area,cat,prio,mark,objid) select number,area,cat,prio,mark,objid from old.main;
insert into title(number,lang,title,missed) select number,lang,title,missed from old.title;
-- insert into ver(number,lang,version,releasedon,isinternal) select number,lang,version,releasedon,isinternal from old.ver;
insert into xmlcache(filename,lastmod) select filename,lastmod from old.xmlcache;
