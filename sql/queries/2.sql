WITH recursive trajectory( userid, pos, start, src, dst, depth ) AS 
( 
  SELECT t.userid, 
         t.tpos, 
         t.utctimestamp AS START, 
         c.cattype      AS src, 
         c.cattype      AS dst, 
         1 
  FROM       trajectories_sl t 
  INNER JOIN categories c ON c.venueid = t.venueid 
  
  UNION ALL 
  
  SELECT t.userid, 
         t.tpos, 
         tt.START  AS START, 
         tt.src    AS src, 
         c.cattype AS dst, 
         tt.depth + 1 
  FROM   trajectories_sl t, 
         trajectory tt, 
         categories c 
  WHERE tt.userid = t.userid 
         AND t.tpos = tt.pos + 1 
         AND c.venueid = t.venueid 
         AND extract(day FROM t.utctimestamp) = extract(day FROM tt.START) 
) 
SELECT userid, 
       START::DATE, 
       pos - depth + 1 AS start_pos, 
       depth AS length 
FROM   trajectory 
WHERE  src = 'Home' 
AND    dst = 'Airport'

