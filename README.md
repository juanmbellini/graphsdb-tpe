#  El paradigma NoSQL - Bases de Datos de Grafos: TP Especial


## 1.1) Crear ambas tablas en PG


```sql
CREATE TABLE public.categories ( 
venueid TEXT, 
venuecategory TEXT, 
latitude DOUBLE PRECISION, 
longitude DOUBLE PRECISION, 
cattype TEXT );

CREATE TABLE public.trajectories ( 
userid INTEGER, 
venueid TEXT, 
utctimestamp TIMESTAMP WITHOUT TIME ZONE, 
tpos BIGINT );
```
## 1.2) Poblar categories con la información provista en el CSV

### 1.2.1) Crear tabla auxiliar a partir de csv

```sql
CREATE TABLE aux ( 
userid INTEGER, 
latitude DOUBLE PRECISION, 
longitude DOUBLE PRECISION, 
utctimestamp TIMESTAMP, 
venueid TEXT, 
venuecategory TEXT, 
cattype TEXT );
```

### 1.2.2) Poblar tabla auxiliar desde csv

`\copy aux FROM './tpgrafo.csv' DELIMITER '|' CSV HEADER`


### 1.2.3) Poblar categories desde tabla auxiliar

```sql
INSERT INTO
   PUBLIC.categories ( venueid, venuecategory, latitude, longitude, cattype ) 
   SELECT
      venueid,
      venuecategory,
      Avg(latitude),
      Avg(longitude),
      cattype 
   FROM
      aux 
   GROUP BY
      venueid,
      venuecategory,
      cattype;
```

## 1.3) Desarrollar un algoritmo 

El mismo se puede ver en: `./data/data_generation/generate.js`

### Manual de `generate.js`

```
Usage: node generate.js <file>

Config options:
	<file>: JSON file containing program configuration
	
File parameters: the following are defaults and format
{
	"users": 1000,
	"interval": ['05/10/2010 08:30:20', '06/10/2010 22:00:00'], // defaults to [now() - 1 year, now()]
	"visits": [0, 5], // per day
	"speed": 5.5,
	"output": `config-<random>.csv`
}

```

## 1.4) Desarrollar un algoritmo Java de Pruning 

Hecho en el punto anterior.

## 1.5) Generar 4 tablas

Correr en el root del proyecto: 

```sh
npm install
node ./data/data_generation/generate.js config.json
```

Para importar los archivos, ejecutar:

```sql
CREATE TABLE public.trajectories_xx ( 
userid INTEGER, 
venueid TEXT, 
utctimestamp TIMESTAMP WITHOUT TIME ZONE, 
tpos BIGINT );
```


Luego setear formato de fechas: 

```sql
set datestyle to European; -- El formato de las fechas es European
```

Insertar los datos desde el .csv: 
```sql 
\copy trajectories_xx FROM './trajectories_xx.csv' DELIMITER ';' CSV HEADER
```

Finalmente crear indices, PKs y FKs:

```sql
ALTER TABLE categories ADD PRIMARY KEY(venueid);
CREATE INDEX xx_user_ix ON trajectories_xx(userid);
ALTER TABLE trajectories_xx ADD CONSTRAINT xx_venue FOREIGN KEY(venueid) REFERENCES categories;
```
