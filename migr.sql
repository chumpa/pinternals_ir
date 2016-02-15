attach database 'notes_old.db' as 'old';
.databases
insert into area(area,text,descr,parent) select area,text,descr,parent from old.area;
insert into xmlcache(filename,lastmod) select filename,lastmod from old.xmlcache;
insert into sld_swcv (name,caption,version,type) select  name,caption,version,type from old.sld_swcv;
insert into main(number,area,cat,prio,mark,objid) select number,area,cat,prio,mark,objid from old.main;
insert into title(number,lang,title,missed) select number,lang,title,missed from old.title;
insert into ver(number,lang,version,releasedon,isinternal) select number,lang,version,releasedon,isinternal from old.ver;

