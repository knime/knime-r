DECLARE @knimebin varbinary(max);
select @knimebin = workspace from ${KnimeRWorkspaceTable};

declare @instance_name nvarchar(100) = @@SERVERNAME, @database_name nvarchar(128) = db_name();

EXEC sp_execute_external_script
@language=N'R',
@script=N'
${RCode}
',
@input_data_1=N'${inputQuery}',
@input_data_1_name=N'knime.in',
@output_data_1_name=N'knime.out',
@params=N'@knimeserialized varbinary(MAX), @knimedbserver nvarchar(100), @knimedbname nvarchar(128)',
@knimeserialized=@knimebin,
@knimedbserver=@instance_name, 
@knimedbname=@database_name
WITH RESULT SETS UNDEFINED;
