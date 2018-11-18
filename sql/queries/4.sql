WITH RECURSIVE trajectory(
	userid,
	pos,
	start,
	src,
	dst,
	length
) AS (
   SELECT
      t.userid,
      t.tpos,
      t.utctimestamp as start,
      t.venueid as src,
      t.venueid as dst,
      1
   FROM
      trajectories_ss t 
      INNER JOIN categories c ON c.venueid = t.venueid 
      UNION ALL
      SELECT
         t.userid,
         t.tpos,
         tt.start as start,
         tt.src as src,
         t.venueid as dst,
         tt.length + 1
      FROM
         trajectories_ss t,
         trajectory tt,
         categories c 
      WHERE
         tt.userid = t.userid
         and t.tpos = tt.pos + 1
         and c.venueid = t.venueid
         and EXTRACT(DAY from t.utctimestamp) = EXTRACT(day from tt.start)
)
SELECT
   t1.userid,
   t1.start::date as s,
   t1.length,
   t1.src,
   t1.dst
FROM trajectory t1
INNER JOIN (
	select
		userid,
		max(c) as l
	from (
		select
			userid,
			utctimestamp::date as s,
			count(*) as c
		from trajectories_ss t1
		group by userid, s
	) t
	group by userid
) tmp ON tmp.userid = t1.userid AND t1.length = tmp.l
