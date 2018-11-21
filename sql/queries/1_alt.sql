select s1.userid, s1.tpos from trajectories_ss s1 
inner join trajectories_ss s2 on (s1.userid = s2.userid and s1.tpos = s2.tpos + 1) 
inner join trajectories_ss s3 on (s1.userid = s3.userid and s2.tpos = s3.tpos + 1) 
inner join categories c1 on c1.venueid = s1.venueid 
inner join categories c2 on c2.venueid = s2.venueid 
inner join categories c3 on c3.venueid = s3.venueid 
where c1.cattype = 'Home' and c2.cattype = 'Station' and c3.cattype = 'Airport'
order by s1.userid, s1.tpos;