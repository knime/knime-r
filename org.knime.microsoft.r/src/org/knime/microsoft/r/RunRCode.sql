DECLARE @modelbin varbinary(max);
select @modelbin = workspace from [db_owner].[KNIME_R_WORKSPACE];

declare @instance_name nvarchar(100) = @@SERVERNAME, @database_name nvarchar(128) = db_name();

EXEC sp_execute_external_script
@language=N'R',
@script=N'
    knime.db.server<-knimedbserver
    knime.db.name<-knimedbname
    rm(knimedbserver,knimedbname)

	knime.model<-readRDS(rawConnection(knimemodelserialized,open="r"))
	knime.db.connection<-paste("Driver=SQL Server;Server=", knime.db.server, ";Database=", knime.db.name, ";uid=${usr};pwd=${pwd}", sep="")

	${userRCode}

	outputTable <- RxOdbcData(connectionString=knime.db.connection, table="${outTableName}")
	rxDataStep(inData=knime.out, outFile=outputTable, overwrite=TRUE)
',
@input_data_1=N'SELECT * from [db_owner].[NewData]',
@input_data_1_name=N'knime.in',
@output_data_1_name=N'knime.out',
@params=N'@knimemodelserialized varbinary(MAX), @knimedbserver nvarchar(100), @knimedbname nvarchar(128)',
@knimemodelserialized=@modelbin,
@knimedbserver=@instance_name, 
@knimedbname=@database_name
WITH RESULT SETS UNDEFINED;
