#ifndef EXPORT_H
#define EXPORT_H

/* Writes the rows of one elevator to <out_dir>/<id>.csv. Returns 0 on success. */
int export_elevator(const char *id, const char *log_path, const char *out_dir);

#endif
