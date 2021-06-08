#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

#include <fcntl.h>
#include <termios.h>
#include <unistd.h>
#include <time.h>
#include <pthread.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>

#define MAX_REQUEST_BUFFER_LENGTH 8192
#define MAX_RESPONSE_BUFFER_LENGTH 65536

#define LEFT -1
#define NEUTRAL 0
#define RIGHT 1
#define ERROR 2

#define ON  1
#define OFF 0

#define N_PROGRAMS 1

#include "strings.h"

char request_buffer[MAX_REQUEST_BUFFER_LENGTH];
char response_buffer[MAX_RESPONSE_BUFFER_LENGTH];

int serial_fd;

int recv_cmd(int);
int short_pause();

int open_serial(const char*);

void process_command(char*, int);

int main(int argc, char** argv) {
  int fd_listen, fd_connect = -1;
  struct sockaddr_in address;
  int recv_result;

  address.sin_family = AF_INET;
  address.sin_port = htons(80);
  address.sin_addr.s_addr = htonl(INADDR_ANY);


  if (argc >= 2) {
    if (strcmp(argv[1], "-h") == 0) {
      printf("Usage: %s [ip address] [port]\n", argv[0]);
      return 0;
    }
    address.sin_addr.s_addr = inet_addr(argv[1]);
  }
  if (argc == 3)
    address.sin_port = htons(atoi(argv[2]));
  if (argc == 4)
    serial_fd = open_serial(argv[3]);
  else
    serial_fd = open_serial("/dev/ttyUSB0");

  printf("IoT-incubator emulator\n");

  fd_listen = socket(AF_INET, SOCK_STREAM, 0);
  if (fd_listen == -1) {
    perror("Socket opening failed");
    return -1;
  } else {
    printf("Socket was opened\n");
  }
  if (bind(fd_listen, (struct sockaddr*)&address, sizeof(address)) == -1) {
    perror("Socket binding failed");
    return -1;
  } else {
    printf("Socket was bound to IP address %s:%hu\n", 
          inet_ntoa(address.sin_addr), ntohs(address.sin_port));
  }
  if (listen(fd_listen, 10) == -1) {
    perror("Socket listening failed");
    return -1;
  }

  while (1) {
    fd_connect = accept(fd_listen, (struct sockaddr*)NULL, NULL);
    if (fd_connect == -1) {
      perror("Connection failed");
      continue;
    } 
    recv_result = recv_cmd(fd_connect);
    close(fd_connect);
    short_pause();
  }

  return 0;
}

int recv_cmd(int fd) {
  int len_buf = 0, header_size = 0;
  int n_headers = 1, len_line = 1;
  char** headers;

  len_buf = recv(fd, request_buffer, MAX_REQUEST_BUFFER_LENGTH, 0);
  if (len_buf == -1)
    return -1;
  else if (len_buf == 0)
    return 0;
  else if (len_buf > MAX_REQUEST_BUFFER_LENGTH) {
    snprintf(response_buffer, MAX_RESPONSE_BUFFER_LENGTH,
            "HTTP/1.1 413 Payload Too Large\r\n"
            "Server: IncubatorGate\r\n"
            "Content-Type: text/html; charset=utf-8\r\n"
            "Content-Length: %d\r\n"
            "\r\n\r\n%s",
            strlen(msg413) + 2, msg413);
    send(fd, response_buffer, MAX_RESPONSE_BUFFER_LENGTH, MSG_EOR);
    return 413;
  }
  if (len_buf < MAX_REQUEST_BUFFER_LENGTH)
    request_buffer[len_buf] = '\0';

  headers = (char**)malloc(n_headers*sizeof(char*));
  headers[0] = (char*)malloc(len_line*sizeof(char));
  for (int i = 0; i < len_buf; i++) {
    if (request_buffer[i] == '\r') 
      continue;
    if (request_buffer[i] == '\0') 
      break;
    if (request_buffer[i] == '\n') {
      if (strlen(headers[n_headers-1]) == 0) {
        header_size = i+1;
        break;
      }
      n_headers++;
      len_line = 1;
      headers = (char**)realloc(headers, n_headers*sizeof(char*));
      headers[n_headers-1] = (char*)malloc(len_line*sizeof(char));
      headers[n_headers-1][0] = '\0';
    } else {
      headers[n_headers-1][len_line-1] = request_buffer[i];
      len_line++;
      headers[n_headers-1] = (char*)realloc(headers[n_headers-1],
                                            len_line*sizeof(char));
      headers[n_headers-1][len_line-1] = '\0';
    }
  }

  if (strstr(headers[0], "GET / ") || strstr(headers[0], "POST / ")) {
    snprintf(response_buffer, MAX_RESPONSE_BUFFER_LENGTH,
            "HTTP/1.1 200 OK\r\n"
            "Server: IncubatorGate\r\n"
            "Content-Type: text/html; charset=utf-8\r\n"
            "Content-Length: %d\r\n"
            "\r\n%s",
            strlen(msgWelcome), msgWelcome);
  } else if (strstr(headers[0], "POST /control")) {
    process_command(request_buffer + header_size, len_buf - header_size);
  } else if (strstr(headers[0], "GET /control")) {
    snprintf(response_buffer, MAX_RESPONSE_BUFFER_LENGTH,
             "HTTP/1.1 200 OK\r\n"
             "Server: IncubatorGate\r\n"
             "Content-Type: text/html; charset=utf-8\r\n"
             "Content-Length: 12\r\n"
             "\r\nmethod_get\r\n");
  } else {
    snprintf(response_buffer, MAX_RESPONSE_BUFFER_LENGTH,
            "HTTP/1.1 404 Not Found\r\n"
            "Server: IncubatorGate\r\n"
            "Content-Type: text/html; charset=utf-8\r\n"
            "Content-Length: %d\r\n"
            "\r\n\r\n%s",
            strlen(msg404) + 2, msg404);
  }

  free(headers);
  send(fd, response_buffer, MAX_RESPONSE_BUFFER_LENGTH, MSG_EOR);
  return 200;
}

int short_pause() {
  struct timespec time;
  time.tv_sec = 0;
  time.tv_nsec = 100000000;
  return nanosleep(&time, &time);
}

int open_serial(const char* dev_name) {
  int fd;
  struct termios io_parameters;
  
  fd = open(dev_name, O_RDWR | O_NOCTTY);
  if (fd == -1)
    return fd;

  tcgetattr(fd, &io_parameters);
  
  cfsetispeed(&io_parameters, B9600);
  cfsetospeed(&io_parameters, B9600);

  cfmakeraw(&io_parameters);
  
  io_parameters.c_cc[VTIME] = 20;
  io_parameters.c_cc[VMIN] = 0;
  tcsetattr(fd, TCSANOW, &io_parameters);

  sleep(1);

  return fd;
}

void process_command(char* buffer, int len_buf) {
  char read_buf[8192] = {0};
  int ret;

  tcflush(serial_fd, TCOFLUSH);
  write(serial_fd, buffer, len_buf);

  ret = read(serial_fd, read_buf, 8192);

  perror("State");
  printf("Q %d %s\r\n", len_buf, buffer);
  printf("A %s\r\n", read_buf);

  snprintf(response_buffer, MAX_RESPONSE_BUFFER_LENGTH,
           "HTTP/1.1 200 OK\r\n"
           "Server: IncubatorGate\r\n"
           "Content-Type: text/html; charset=utf-8\r\n"
           "Content-Length: %d\r\n"
            "\r\n%s",
           ret, read_buf);
}


