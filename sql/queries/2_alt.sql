select s1.userid, s1.utctimestamp::DATE, s1.tpos, s2.tpos - s1.tpos + 1 as length from trajectories_ss s1 
inner join trajectories_ss s2 on (s1.userid = s2.userid and s1.tpos < s2.tpos and extract(DAY FROM s1.utctimestamp) = extract(DAY FROM s2.utctimestamp)) 
inner join categories c1 on c1.venueid = s1.venueid 
inner join categories c2 on c2.venueid = s2.venueid 
where c1.cattype = 'Home' and c2.cattype = 'Airport'
order by length, userid;