DECLARE @modelbin varbinary(max);
select @modelbin = workspace from db_owner.KNIME_R_WORKSPACE;

EXEC sp_execute_external_script
@language=N'R',
@script=N'knime.model<-readRDS(rawConnection(knimemodelserialized,open="r"));${userRCode}',
@input_data_1=N'select * from db_accessadmin.NewData',
@input_data_1_name=N'knime.in',
@output_data_1_name=N'knime.out',
@params=N'@knimemodelserialized varbinary(MAX) OUTPUT',
@knimemodelserialized=@modelbin OUTPUT
WITH RESULT SETS UNDEFINED;