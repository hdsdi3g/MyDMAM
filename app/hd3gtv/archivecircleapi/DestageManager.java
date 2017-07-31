/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.archivecircleapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.archivecircleapi.ACTapeAudit.TapeAuditEvent;
import hd3gtv.archivecircleapi.ACTransferJob.Status;
import hd3gtv.mydmam.Loggers;

public class DestageManager {
	
	private final Logger log = Logger.getLogger(DestageManager.class);
	
	private ACAPI acapi;
	private Function<List<ACTape>, Boolean> tapeRequest;
	private BiConsumer<ACFile, Exception> onError;
	private ScheduledExecutorService scheduled_ex_service;
	
	private List<FileDestageJob> all_file_jobs;
	private List<ACTape> all_wanted_tapes;
	
	private ScheduledFuture<?> regular_refresh_tape_audit;
	private final AtomicLong last_tape_audit_refresh_date = new AtomicLong(System.currentTimeMillis());
	
	/**
	 * @param acapi Engine
	 * @param tapeRequest Ask to end-user to insert a tape in a library. @return true if it should be ok, or false to cancel.
	 */
	public DestageManager(ACAPI acapi, Function<List<ACTape>, Boolean> tapeRequest, BiConsumer<ACFile, Exception> onError) {
		this.acapi = acapi;
		if (acapi == null) {
			throw new NullPointerException("\"acapi\" can't to be null");
		}
		this.tapeRequest = tapeRequest;
		if (tapeRequest == null) {
			throw new NullPointerException("\"tapeRequest\" can't to be null");
		}
		this.onError = onError;
		if (onError == null) {
			throw new NullPointerException("\"onError\" can't to be null");
		}
		
		scheduled_ex_service = Executors.newSingleThreadScheduledExecutor();
		all_file_jobs = Collections.synchronizedList(new ArrayList<>());
		all_wanted_tapes = Collections.synchronizedList(new ArrayList<>());
	}
	
	/**
	 * @param file accept all-accessibility
	 * @param external_id will be set on destage list and for the BiConsumer
	 * @param onAvailability callback when it's done.
	 * @param onCantRestore this file can't be restored actually (error status or timeout)
	 * @param timeout in ms
	 * @return null if the file is now online (no created job).
	 */
	public FileDestageJob addFileToDestage(ACFile file, String external_id, BiConsumer<ACFile, String> onAvailability, BiConsumer<ACFile, String> onCantRestore, long timeout, TimeUnit unit) {
		if (file == null) {
			throw new NullPointerException("\"file\" can't to be null");
		}
		if (onAvailability == null) {
			throw new NullPointerException("\"onAvailability\" can't to be null");
		}
		if (onCantRestore == null) {
			throw new NullPointerException("\"onCantRestore\" can't to be null");
		}
		
		if (file.accessibility == ACAccessibility.ONLINE) {
			log.debug("Destage an ONLINE file: " + file + " (do nothing)");
			onAvailability.accept(file, external_id);
			return null;
		} else {
			FileDestageJob result = new FileDestageJob(file, external_id, onAvailability, onCantRestore, unit.toMillis(timeout));
			all_file_jobs.add(result);
			return result;
		}
	}
	
	public class FileDestageJob implements Runnable {
		private ACFile file;
		private BiConsumer<ACFile, String> onAvailability;
		private BiConsumer<ACFile, String> onCantRestore;
		private long max_date;
		private String external_id;
		
		private List<String> wanted_tape_barcodes;
		private ACTransferJob destage_job;
		private ScheduledFuture<?> last_time_check;
		
		private FileDestageJob(ACFile file, String external_id, BiConsumer<ACFile, String> onAvailability, BiConsumer<ACFile, String> onCantRestore, long timeout) {
			this.file = file;
			this.onAvailability = onAvailability;
			this.onCantRestore = onCantRestore;
			this.max_date = System.currentTimeMillis() + timeout;
			this.external_id = external_id;
			log.info("Create a destage Job for " + external_id + ": " + file + ", job expiration the " + Loggers.dateLog(max_date));
			run();
		}
		
		public synchronized ACAccessibility getActualAccessibility() {
			return file.accessibility;
		}
		
		/**
		 * @return [Barcode1, ...] or null
		 */
		public synchronized Stream<String> wantsSomeTapesActually() {
			if (wanted_tape_barcodes == null) {
				return null;
			}
			if (wanted_tape_barcodes.isEmpty()) {
				return null;
			}
			return wanted_tape_barcodes.stream();
		}
		
		public long getJobExpirationDate() {
			return max_date;
		}
		
		/**
		 * @return can be null
		 */
		public synchronized String getDestageJobStatus() {
			if (destage_job == null) {
				return null;
			}
			
			StringBuilder sb = new StringBuilder();
			if (destage_job.running) {
				sb.append("Running ");
			}
			
			sb.append(destage_job.status + " ");
			
			try {
				sb.append(destage_job.files.get(0).status);
			} catch (Exception e) {
			}
			
			return sb.toString();
		}
		
		public void run() {
			try {
				log.trace("Start to check destage status for " + external_id + ", " + file);
				file = acapi.getFile(file.share, file.path, false);
				
				if (file.accessibility == ACAccessibility.ONLINE) {
					log.info("File " + external_id + ", " + file + " is now ONLINE");
					throwEnd();
					return;
				} else if (file.accessibility == ACAccessibility.NEARLINE) {
					if (destage_job == null) {
						log.info("Start destage for " + external_id + ", " + file + " in " + acapi.getDefaultDestageNode());
						destage_job = acapi.destage(file, external_id, false, acapi.getDefaultDestageNode());
						if (destage_job == null) {
							throw new IOException("Can't do a destage request for " + external_id + " in " + acapi.getDefaultDestageNode());
						}
						log.debug("Destage id is " + destage_job.id + " for " + external_id + ", " + file);
					} else {
						destage_job = acapi.getTransfertJob(destage_job.id);
						if (destage_job.running == false) {
							if (destage_job.status == Status.DONE) {
								throwError(new Exception("Impossible error: file " + file + " is destaged but not online"));
								return;
							} else if (destage_job.status == Status.STOPPED) {
								log.warn("Don't destage " + external_id + ", " + file + " because it's STOPPED");
								throwImpossible();
								return;
							} else {
								log.trace("Destage " + destage_job.id + " is " + destage_job.status + " (not running) for " + external_id + ", " + file);
							}
						} else {
							log.trace("Destage " + destage_job.id + " is " + destage_job.status + " (running) for " + external_id + ", " + file);
						}
					}
				} else {
					if (wanted_tape_barcodes != null) {
						wanted_tape_barcodes = file.getTapeBarcodeLocations();
						log.info("Needs tape(s) " + wanted_tape_barcodes.stream().collect(Collectors.joining(", ")) + " before destage " + external_id + ", " + file);
						askToInsertTape(wanted_tape_barcodes);
						last_time_check = scheduled_ex_service.schedule(() -> {
							/**
							 * Solution for abandoned get tape requests: at the max wait time, refresh file status and re-run this.
							 */
							log.info("Abandoned get tape request will try to restart operation for " + external_id + ", " + file);
							
							file = acapi.getFile(file.share, file.path, false);
							run();
						}, max_date - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
						return;
					}
				}
			} catch (Exception e) {
				log.error("Generic error (cancel destaging) for " + external_id + ", " + file, e);
				throwError(e);
				return;
			}
			
			if (System.currentTimeMillis() < max_date) {
				int time = 1 + (all_file_jobs.size() / 50);
				log.trace("Schedule next watching destage in " + time + " min for " + external_id + ", " + file);
				scheduled_ex_service.schedule(this, time, TimeUnit.MINUTES);
			} else {
				log.warn("Destage job take to long time, cancel it for " + external_id + ", " + file);
				throwImpossible();
			}
		}
		
		private void removeTransfertJob() {
			if (destage_job != null) {
				try {
					log.trace("Remove destage " + destage_job.id + " job from ACAPI (it's was for " + external_id + ", " + file + ")");
					acapi.deleteTransfertJob(destage_job.id);
				} catch (Exception e) {
				}
			}
		}
		
		private void throwEnd() {
			all_file_jobs.remove(this);
			removeTransfertJob();
			if (last_time_check != null) {
				last_time_check.cancel(false);
				last_time_check = null;
			}
			onAvailability.accept(file, external_id);
		}
		
		private void throwImpossible() {
			all_file_jobs.remove(this);
			removeTransfertJob();
			if (last_time_check != null) {
				last_time_check.cancel(false);
				last_time_check = null;
			}
			onCantRestore.accept(file, external_id);
		}
		
		private void throwError(Exception e) {
			throwImpossible();
			onError.accept(file, e);
		}
		
	}
	
	public Stream<ACFile> getCurrentList() {
		return all_file_jobs.stream().map(j -> {
			return j.file;
		});
	}
	
	private Runnable refreshTapesAudit = () -> {
		log.trace("Start tapes audit refresh (the last was the " + Loggers.dateLog(last_tape_audit_refresh_date.get()) + ")");
		
		ArrayList<ACTapeAudit> last_movments = acapi.getLastTapeAudit(last_tape_audit_refresh_date.getAndSet(System.currentTimeMillis()));
		
		/**
		 * Get all barcodes from tapes added in a library
		 */
		List<String> recently_added_tapes_barcodes = last_movments.stream().filter(last_mvt -> {
			return last_mvt.event == TapeAuditEvent.MOVED_INTO_LIBRARY;
		}).map(last_mvt -> {
			return last_mvt.barcode;
		}).collect(Collectors.toList());
		
		if (last_movments.isEmpty()) {
			log.trace("Tape audit don't found event in library");
		} else {
			if (recently_added_tapes_barcodes.isEmpty()) {
				if (log.isTraceEnabled()) {
					log.trace("Tape audit found event(s) in library:" + last_movments.stream().map(last_mvt -> {
						return last_mvt.barcode + " > " + last_mvt.event;
					}).collect(Collectors.joining(", ")));
				}
			} else {
				log.info("Tape audit found tape(s) moved into library: " + recently_added_tapes_barcodes.stream().collect(Collectors.joining(", ")));
			}
		}
		
		all_file_jobs.stream().filter(f_j -> {
			return f_j.wanted_tape_barcodes != null && f_j.destage_job == null;
		}).filter(f_j -> {
			return f_j.wanted_tape_barcodes.stream().anyMatch(barcode -> {
				return recently_added_tapes_barcodes.contains(barcode);
			});
		}).forEach(f_j -> {
			if (log.isTraceEnabled()) {
				log.trace("Tape audit found some needed tape(s) " + f_j.wanted_tape_barcodes.stream().collect(Collectors.joining(", ")) + " required by " + f_j.external_id + ", " + f_j.file + " are now in library");
			}
			f_j.wanted_tape_barcodes = null;
			f_j.run();
		});
		
		all_wanted_tapes.removeIf(tape -> {
			return recently_added_tapes_barcodes.stream().anyMatch(barcode -> {
				return barcode.equalsIgnoreCase(tape.barcode);
			});
		});
		
		if (all_wanted_tapes.isEmpty() | all_file_jobs.isEmpty()) {
			if (all_file_jobs.isEmpty()) {
				all_wanted_tapes.clear();
			}
			log.debug("Tape audit don't needs to continue to check because job lists are now empty.");
			regular_refresh_tape_audit.cancel(false);
		}
	};
	
	private void askToInsertTape(List<String> barcodes) {
		/**
		 * Get barcodes from tapes.
		 */
		List<String> all_wanted_barcodes = all_wanted_tapes.stream().map(tape -> {
			return tape.barcode;
		}).collect(Collectors.toList());
		
		if (barcodes.stream().anyMatch(b -> {
			return all_wanted_barcodes.stream().anyMatch(c_b -> {
				return c_b.equals(b);
			});
		})) {
			/**
			 * Some barcodes was wanted...
			 */
			return;
		}
		
		/**
		 * Actually, neither barcodes was wanted.
		 * So, let's go to ask them.
		 */
		
		List<ACTape> new_tapes = barcodes.stream().map(barcode -> {
			return acapi.getTape(barcode);
		}).collect(Collectors.toList());
		
		if (tapeRequest.apply(new_tapes) == false) {
			return;
		}
		
		all_wanted_tapes.addAll(new_tapes);
		
		if (regular_refresh_tape_audit != null) {
			if (regular_refresh_tape_audit.isCancelled() == false && regular_refresh_tape_audit.isDone() == false) {
				return;
			}
		}
		
		log.debug("Start tape audit regular refresh for wait one of tape(s) in " + barcodes.stream().collect(Collectors.joining(", ")));
		regular_refresh_tape_audit = scheduled_ex_service.scheduleWithFixedDelay(refreshTapesAudit, 60, 10, TimeUnit.SECONDS);
	}
	
}
