select s3.userid, s3.utctimestamp::DATE as ddate, s3.tpos, s4.tpos - s3.tpos + 1 as lengthh
from trajectories_ss s3 
inner join trajectories_ss s4 on (s3.userid = s4.userid and s3.tpos < s4.tpos and extract(DAY FROM s3.utctimestamp) = extract(DAY FROM s4.utctimestamp)) 
inner join categories c3 on c3.venueid = s3.venueid 
inner join categories c4 on c4.venueid = s4.venueid 
inner join (
	--FROM HERE
	select aaux.userid as userid, max(aaux.length) as length
	from(
		select s1.userid, s1.utctimestamp::DATE as ddate, s1.tpos, s2.tpos - s1.tpos + 1 as length from trajectories_ss s1 
		inner join trajectories_ss s2 on (s1.userid = s2.userid and s1.tpos < s2.tpos and extract(DAY FROM s1.utctimestamp) = extract(DAY FROM s2.utctimestamp)) 
		inner join categories c1 on c1.venueid = s1.venueid 
		inner join categories c2 on c2.venueid = s2.venueid 
		order by length, userid
		) as aaux
	group by aaux.userid
	order by aaux.userid
	--TO HERE OBTAIN MAX DAY LENGTH PER USER
) as innerr on innerr.userid = s3.userid 
where s4.tpos - s3.tpos + 1 = innerr.length
order by s3.userid;
